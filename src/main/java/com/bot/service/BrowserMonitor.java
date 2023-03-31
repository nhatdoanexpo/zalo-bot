package com.bot.service;

import com.bot.config.BrowserConfig;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
@Slf4j
public class BrowserMonitor {
    private static final Logger logger = LoggerFactory.getLogger(BrowserMonitor.class);


    final JobHandler jobHandler;
    final BrowserConfig browserConfig;

    public String profile = "user_99";

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
                        this.handleBrowser(profile);
                    } catch (Exception e) {
                        logger.error("BrowserMonitor >> startBrowser >> Exception", e);
                         } finally {
                        blockingKey.remove(key);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("BrowserMonitor >> startBrowser >> Exception", e);
   }

    }


    void handleBrowser(String profile) throws IOException {
        var driver = browserConfig.getChromeDriver(profile);
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.get(ZALO_URL);
            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            var js = (JavascriptExecutor) driver;

            // Vòng lặp vô hạn để chờ và xử lý message
            while (true) {
                // Chờ cho đến khi có sự kiện "CLIENT_IN"
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofDays(1));
                By clientInEvent = By.xpath("//body[@data-client-in]");
                wait.until(ExpectedConditions.presenceOfElementLocated(clientInEvent));

                // Lấy ra message và xử lý
                Object result = js.executeScript("return document.querySelector('body').dataset.clientIn");
                if (result instanceof Map) {
                    Map<String, Object> message = (Map<String, Object>) result;
                    // Xử lý message ở đây
                    // ...

                    // Gửi lại message qua "CLIENT_OUT"
                    message.put("status", "DONE");
                    js.executeScript("document.querySelector('body').dispatchEvent(new CustomEvent('CLIENT_OUT', { detail: " + new Gson().toJson(message) + " }))");

                }
            }

//            var jsScript = """
//                    window.addEventListener('CLIENT_IN', function(event) {
//                         var message = event.detail;
//                         localStorage.setItem('postMessageKey', message);
//                    });
//                    """;
//            js.executeScript(jsScript);
//            var message = "";
//            while (!message.equals("exist")) {
//                Thread.sleep(TimeUnit.SECONDS.toMillis(3));
//                var postMessage = (String) js.executeScript("return window.localStorage.getItem('postMessageKey');");
//                message = StringUtils.defaultIfBlank(postMessage, "");
//                log.log(Level.INFO, "BrowserMonitor >> handleBrowser >> receive message: {0}", message);
//                if (StringUtils.isNotBlank(message)) {
//                    jobHandler.sendAction(driver, message);
//                }
//
//            }

        } catch (Exception e) {
            logger.error("BrowserMonitor >> handleBrowser >> Exception:", e);

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
