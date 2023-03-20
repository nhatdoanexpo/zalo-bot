package com.bot.service;

import com.bot.config.BrowserConfig;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

@Service
@Log
public class BrowserMonitor {

    final JobHandler jobHandler;
    final BrowserConfig browserConfig;

    private static final String ZALO_URL = "https://chat.zalo.me";


    private final ConcurrentHashMap<String, AtomicBoolean> blockingKey = new ConcurrentHashMap<>();

    public BrowserMonitor(BrowserConfig browserConfig, JobHandler jobHandler) {
        this.browserConfig = browserConfig;
        this.jobHandler = jobHandler;
    }

    @Scheduled(fixedDelay = 500)
    void startBrowser() {
        try {
            var key = "LOCK";
            if (blockActionByKey(blockingKey, key)) {
                Executors.newSingleThreadExecutor().submit(() -> {
                    try {
                        this.handleBrowser();
                    } catch (Exception e) {
                        log.log(Level.WARNING, "BrowserMonitor >> startBrowser >> Exception:", e);
                    } finally {
                        blockingKey.remove(key);
                    }
                });
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "BrowserMonitor >> startBrowser >> Exception:", e);
        }

    }


    void handleBrowser() throws IOException {
        var driver = browserConfig.getChromeDriver("user_99");
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.get(ZALO_URL);
            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            var js = (JavascriptExecutor) driver;
            js.executeScript("localStorage.removeItem('postMessageKey');");
            var jsScript = """
                    window.addEventListener('message', function(event) {
                         var message = event.data;
                         localStorage.setItem('postMessageKey', message);
                    });
                    """;
            js.executeScript(jsScript);
            var message = "";
            while (!message.equals("exist")) {
                Thread.sleep(TimeUnit.SECONDS.toMillis(3));
                var postMessage = (String) js.executeScript("return window.localStorage.getItem('postMessageKey');");
                message = StringUtils.defaultIfBlank(postMessage, "");
                log.log(Level.INFO, "BrowserMonitor >> handleBrowser >> receive message: {0}", message);
                if (StringUtils.isNotBlank(message)) {
                    jobHandler.sendAction(driver, message);
                }

            }

        } catch (Exception e) {
            log.log(Level.WARNING, "BrowserMonitor >> handleBrowser >> Exception:", e);
            Thread.currentThread().interrupt();
        } finally {
            driver.quit();
        }
    }


    public boolean blockActionByKey(Map<String, AtomicBoolean> trackingMap, String trackingKey) {
        var newBlock = new AtomicBoolean(true);
        var existingBlock = Optional.ofNullable(trackingMap.putIfAbsent(trackingKey, newBlock));
        if (existingBlock.isPresent()) {
            newBlock = existingBlock.get();
        }
        var result = newBlock;
        return result.compareAndSet(true, false);

    }

}
