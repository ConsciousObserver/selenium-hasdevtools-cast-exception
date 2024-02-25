package com.example.hasdevtoolscastexception;

import java.util.Optional;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class HasdevtoolsCastExceptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(HasdevtoolsCastExceptionApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
            SeleniumContainerSetup seleniumContainerSetup = null;
            try {
                seleniumContainerSetup = new SeleniumContainerSetup();

                testDevTools(seleniumContainerSetup.getBrowserWebDriverContainer());
            } finally {
                if (seleniumContainerSetup != null) {
                    seleniumContainerSetup.stopContainers();
                }
            }
        };
    }

    void testDevTools(BrowserWebDriverContainer<?> browserWebDriverContainer) {
        RemoteWebDriver webDriver = null;

        try {
            ChromeOptions capabilities = new ChromeOptions().addArguments("--headless");
            webDriver = new RemoteWebDriver(browserWebDriverContainer.getSeleniumAddress(), capabilities);
            //            webDriver.getCapabilities().getCapability("se:cdp");
            WebDriver augmentedWebDriver = new Augmenter().augment(webDriver);
            DevTools devTools = ((HasDevTools) augmentedWebDriver).getDevTools();
            devTools.createSession();

            devTools.send(Network.enable(Optional.of(1000000), Optional.empty(), Optional.empty()));

            devTools.addListener(Network.responseReceived(), responseReceived -> {
                log.info("status: {}, Url: {}", responseReceived.getResponse().getStatus(),
                        responseReceived.getResponse().getUrl());
            });

            System.out.println("VNC address : " + browserWebDriverContainer.getVncAddress());
            webDriver.get("https://stackoverflow.com");

            System.out.println(webDriver.getTitle());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            if (webDriver != null) {
                webDriver.quit();
            }
        }
    }
}

@Slf4j
@Data
class SeleniumContainerSetup {
    private SocatContainer tcpProxySocatContainer;

    private BrowserWebDriverContainer<?> browserWebDriverContainer;

    public SeleniumContainerSetup() {
        startContainers();
    }

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
}