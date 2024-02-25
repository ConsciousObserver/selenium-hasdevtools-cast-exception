package com.example.hasdevtoolscastexception;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v118.network.Network;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.SocatContainer;

//@Testcontainers
public class HasdevtoolsCastExceptionApplicationTest {
    org.testcontainers.containers.Network socatNetwork = org.testcontainers.containers.Network.newNetwork();

    //    @Container
    //    SocatContainer socatContainer = new SocatContainer()
    //            .withTarget(2000, "chrome", 4444)
    //            .withNetwork(socatNetwork);

    //    {
    //        network = Network.newNetwork();
    //
    //        socatContainer = new SocatContainer()
    //                .withTarget(2000, "chrome", 4444)
    //                .withNetwork(network);
    //        socatContainer.start();
    //    }

    //    @Container
    //    BrowserWebDriverContainer<?> browserWebDriverContainer = new BrowserWebDriverContainer<>()
    //            .dependsOn(socatContainer)
    //            .withCapabilities(new ChromeOptions())
    //            .withEnv("SE_NODE_GRID_URL", String.format("%s:%d", socatContainer.getHost(), socatContainer.getMappedPort(2000)))
    //            .withNetworkAliases("chrome")
    //            .withNetwork(socatNetwork);
    //            .withRecordingMode(VncRecordingMode.RECORD_ALL, new File("target"))
    //            .withRecordingFileFactory(new DefaultRecordingFileFactory());

    @Test
    void test() {
        SocatContainer socatContainer = null;
        BrowserWebDriverContainer<?> browserWebDriverContainer = null;
        RemoteWebDriver webDriver = null;

        try {
            socatContainer = new SocatContainer()
                    .withTarget(2000, "chrome", 4444)
                    .withNetwork(socatNetwork);
            //                    .withStartupCheckStrategy(
            //                            new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(60))
            //                        );

            socatContainer.start();

            browserWebDriverContainer = new BrowserWebDriverContainer<>()
                    .dependsOn(socatContainer)
                    .withCapabilities(new ChromeOptions())
                    .withEnv("SE_NODE_GRID_URL",
                            String.format("http://%s:%d", socatContainer.getHost(), socatContainer.getMappedPort(2000)))
                    .withNetworkAliases("chrome")
                    .withNetwork(socatNetwork);

            browserWebDriverContainer.start();

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
            //            System.out.println("Devtools: " + devTools);

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

            if (socatContainer != null) {
                socatContainer.stop();
            }

            if (browserWebDriverContainer != null) {
                browserWebDriverContainer.stop();
            }
        }

        //        System.out.println("Running Test");
        //        RemoteWebDriver webDriver = null;
        //        try {
        //            ChromeOptions capabilities = new ChromeOptions().addArguments("--headless");
        //            webDriver = new RemoteWebDriver(browserWebDriverContainer.getSeleniumAddress(), capabilities);
        ////            webDriver.getCapabilities().getCapability("se:cdp");
        //            WebDriver augmentedWebDriver = new Augmenter().augment(webDriver);
        //            DevTools devTools = ((HasDevTools) augmentedWebDriver).getDevTools();
        //            devTools.createSession();
        ////            System.out.println("Devtools: " + devTools);
        //            
        //            System.out.println("VNC address : " + browserWebDriverContainer.getVncAddress());
        //            webDriver.get("https://stackoverflow.com");
        //
        //            System.out.println(webDriver.getTitle());
        //            
        ////            webDriver.getClass().getInterfaces();
        //        } catch (Exception ex) {
        //            ex.printStackTrace();
        //            throw ex;
        //        } finally {
        //            if (webDriver != null) {
        //                webDriver.quit();
        //            }
        //        }
    }
}