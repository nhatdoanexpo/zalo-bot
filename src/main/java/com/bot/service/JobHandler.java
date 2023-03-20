package com.bot.service;

import com.bot.model.PostMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.logging.Level;

@Service
@Log
public class JobHandler {

    private static final String ACTION_KEY = "postMessageKey";
    final ObjectMapper objectMapper = new ObjectMapper();

    public void sendAction(WebDriver driver, String message) {
        try {

            var postMessage = convertMessage(message);
            if (PostMessage.Status.NEW.equals(postMessage.getMessageStatus())) {
                log.log(Level.INFO, "JobHandler >> sendAction >> new action >> {0}", message);
                // after done remove set message in localStorage
                postMessage.setMessageStatus(PostMessage.Status.DONE);
                var json = objectMapper.writeValueAsString(postMessage);
                var js = (JavascriptExecutor) driver;
                js.executeScript("window.localStorage.setItem(arguments[0], arguments[1]);", ACTION_KEY, json);
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "JobHandler >> sendAction >> Exception:", e);
        }

    }

    public PostMessage convertMessage(String message) {
        try {
            return objectMapper.readValue(message, PostMessage.class);
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("JobHandler >> convertMessage >> can not convert message from: {0} >> Exception:", message), e);
            return new PostMessage();
        }
    }

}
