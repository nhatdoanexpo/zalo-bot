package com.bot.service;

import com.bot.model.PostDataMessage;
import com.bot.model.PostMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

import static com.bot.model.PostDataMessage.ActionType;

@Service
@Slf4j
public class JobHandler {
    private static final Logger logger = LoggerFactory.getLogger(JobHandler.class);

    private static final String ACTION_KEY = "postMessageKey";
    final ObjectMapper objectMapper = new ObjectMapper();

    public void sendAction(WebDriver driver, String message) {
        try {
            var postMessage = convertActionMessage(message);
            ActionType actionType = postMessage.getActionType();
          switch (actionType){
              case SEND_KEY:
                  break;
              case UPDATE_PROFILE:
                  break;
              default:
                  logger.info( "not found action >> {0}", message);
                  break;
          }
            postMessage.getDetail().addProperty("status","DONE");

            logger.info( "JobHandler >> sendAction >> new action >> {0}", message);
            var json = objectMapper.writeValueAsString(postMessage);
            var js = (JavascriptExecutor) driver;
            js.executeScript("document.querySelector('body').dispatchEvent(new CustomEvent('CLIENT_OUT', arguments[0] ))",json);

        } catch (Exception e) {
            logger.info("JobHandler >> sendAction >> Exception:", e);
        }

    }



    public PostMessage convertMessage(String message) {
        try {
            return objectMapper.readValue(message, PostMessage.class);
        } catch (Exception e) {
            log.error(MessageFormat.format("JobHandler >> convertMessage >> can not convert message from: {0} >> Exception:", message), e);
            return new PostMessage();
        }
    }

    public PostDataMessage convertActionMessage(String message) {
        try {
            return objectMapper.readValue(message, PostDataMessage.class);
        } catch (Exception e) {
            log.error( MessageFormat.format("JobHandler >> convertMessage >> can not convert message from: {0} >> Exception:", message), e);
            return new PostDataMessage();
        }
    }

}
