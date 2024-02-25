package com.example.hasdevtoolscastexception;

import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v118.network.Network;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.SocatContainer;

//@Testcontainers
public class HasdevtoolsCastExceptionApplicationTest {
    static Logger log = LoggerFactory.getLogger(HasdevtoolsCastExceptionApplicationTest.class);

    static org.testcontainers.containers.Network socatNetwork = org.testcontainers.containers.Network.newNetwork();

    static SocatContainer tcpProxySocatContainer;

    static BrowserWebDriverContainer<?> browserWebDriverContainer;

    @BeforeAll
    static void setUp() {
        log.info("**************** Test setup start");

        log.info("**************** Starting Socat TCP proxy container");

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

    @AfterAll
    static void tearDown() {
        log.info("**************** Stopping containers");
        
        if (tcpProxySocatContainer != null) {
            tcpProxySocatContainer.stop();
        }

        if (browserWebDriverContainer != null) {
            browserWebDriverContainer.stop();
        }
    }

    @Test
    void test() {
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
                System.out.println("status: " + responseReceived.getResponse().getStatus() + ", Url: "
                        + responseReceived.getResponse().getUrl());
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
