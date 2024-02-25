package com.example.hasdevtoolscastexception;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v118.network.Network;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.SocatContainer;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class HasdevtoolsCastExceptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(HasdevtoolsCastExceptionApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
            try (SeleniumContainerSetup seleniumContainerSetup = new SeleniumContainerSetup();) {
                ClassLoader parentThreadContextClassLoader = Thread.currentThread().getContextClassLoader();

                CompletableFuture
                        .supplyAsync(() -> {
                            log.info("**************** ContexClassLoader:::: Parent: {}, Child: {}",
                                    parentThreadContextClassLoader,
                                    Thread.currentThread().getContextClassLoader());

                            if (parentThreadContextClassLoader != Thread.currentThread().getContextClassLoader()) {
                                log.warn(
                                        "**************** "
                                                + "CURRENT THREAD'S CLASS LOADER IS DIFFRENT FROM PARENT."
                                                + " Selenium driver Aumentation will fail if child thread's"
                                                + " contextClassLoader is not set to parent's");
                            }

                            // This line must be uncommented for selenium driver augmentation to work in Spring Boot's runnable JAR. 
                            // Otherwise we get class cast exception with HasDevTools when not running on main thread.
                            Thread.currentThread().setContextClassLoader(parentThreadContextClassLoader);

                            return testDevTools(seleniumContainerSetup.getBrowserWebDriverContainer());
                        })
                        .get();
            }

            log.info("Shutting down");

            System.exit(0);
        };
    }

    /**
     * Connects to Chromium browser in test container with RemoteWebDriver and
     * uses DevTools to print few network requests. However, casting to
     * HasDevTools fails in runnable JAR when this method is run on a new
     * thread.
     * 
     * @param browserWebDriverContainer
     * @return
     */
    String testDevTools(BrowserWebDriverContainer<?> browserWebDriverContainer) {
        RemoteWebDriver webDriver = null;

        try {
            webDriver = new RemoteWebDriver(browserWebDriverContainer.getSeleniumAddress(), new ChromeOptions());

            WebDriver augmentedWebDriver = new Augmenter().augment(webDriver);

            // Fails to cast when run with in runnable JAR
            DevTools devTools = ((HasDevTools) augmentedWebDriver).getDevTools();
            devTools.createSession();

            devTools.send(Network.enable(Optional.of(1000000), Optional.empty(), Optional.empty()));

            devTools.addListener(Network.responseReceived(), responseReceived -> {

                String url = responseReceived.getResponse().getUrl();

                if (url.endsWith(".js")) {
                    log.info("status: {}, Url: {}", responseReceived.getResponse().getStatus(),
                            url);
                }
            });

            webDriver.get("https://stackoverflow.com");

            log.info("Title: {}", webDriver.getTitle());
        } finally {
            if (webDriver != null) {
                webDriver.quit();
            }
        }

        return "done";
    }
}

/**
 * Uses TestContainers for selenium to get RemoteWebDriver. It needs local
 * running Docker.
 *
 */
@Slf4j
@Data
class SeleniumContainerSetup implements Closeable {
    /**
     * Since it's not possible to get the selenium container's port mapped on
     * Docker host, we use this TCP proxy to do that. Now our selenium driver
     * traffic goes through the proxy. Socat proxy container allows us to access
     * host exposed port.
     * 
     * <p>
     * See below answer on Github issues
     * <p>
     * <a href
     * ="https://github.com/testcontainers/testcontainers-java/issues/7242#issuecomment-1644155873">https://github.com/testcontainers/testcontainers-java/issues/7242#issuecomment-1644155873</a>
     */
    private SocatContainer tcpProxySocatContainer;

    /**
     * Container where Chrome browser is running. We use RemoteWebDriver to
     * connect to it. It also starts a vnc-recorder container, which can be used
     * to record videos of selenium sessions.
     */
    private BrowserWebDriverContainer<?> browserWebDriverContainer;

    public SeleniumContainerSetup() {
        startContainers();
    }

    /**
     * Starts TCP proxy and selenium container
     */
    private void startContainers() {
        log.info("**************** Test setup start");

        log.info("**************** Starting Socat TCP proxy container");

        org.testcontainers.containers.Network socatNetwork = org.testcontainers.containers.Network.newNetwork();

        tcpProxySocatContainer = new SocatContainer()
                .withTarget(2000, "chrome", 4444)
                .withNetwork(socatNetwork);

        tcpProxySocatContainer.start();

        log.info("**************** Starting Selenium  container");

        browserWebDriverContainer = new BrowserWebDriverContainer<>()
                .dependsOn(tcpProxySocatContainer)
                .withCapabilities(new ChromeOptions())
                .withEnv("SE_NODE_GRID_URL",
                        String.format("http://%s:%d", tcpProxySocatContainer.getHost(),
                                tcpProxySocatContainer.getMappedPort(2000)))
                .withNetworkAliases("chrome")
                .withNetwork(socatNetwork);

        browserWebDriverContainer.start();

        log.info("**************** Test setup end");
    }

    void stopContainers() {
        log.info("**************** Stopping containers");

        if (tcpProxySocatContainer != null) {
            tcpProxySocatContainer.stop();
        }

        if (browserWebDriverContainer != null) {
            browserWebDriverContainer.stop();
        }
    }

    /**
     * Implemented to use with try-with-resources.
     */
    @Override
    public void close() throws IOException {
        stopContainers();
    }
}