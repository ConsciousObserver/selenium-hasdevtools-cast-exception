package com.example.hasdevtoolscastexception;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode;
import org.testcontainers.containers.DefaultRecordingFileFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class HasdevtoolsCastExceptionApplicationTest {

    @Container
    BrowserWebDriverContainer<?> browserWebDriverContainer = new BrowserWebDriverContainer<>()
            .withCapabilities(new ChromeOptions())
            .withRecordingMode(VncRecordingMode.RECORD_ALL, new File("target"))
            .withRecordingFileFactory(new DefaultRecordingFileFactory());

    @Test
    void test() {
        System.out.println("Running Test");
        RemoteWebDriver webDriver = null;
        try {
            webDriver = new RemoteWebDriver(browserWebDriverContainer.getSeleniumAddress(), new ChromeOptions());
            //        browserWebDriverContainer.start();

            System.out.println("VNC address : " + browserWebDriverContainer.getVncAddress());
            webDriver.get("https://stackoverflow.com");

            System.out.println(webDriver.getTitle());
        } finally {
            if (webDriver != null) {
                webDriver.quit();
            }
        }
    }
}
