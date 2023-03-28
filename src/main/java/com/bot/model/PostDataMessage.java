package com.bot.model;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDataMessage {

    private ActionType actionType;
    private JsonObject detail;

    public static enum ActionType {
        SEND_KEY, UPDATE_PROFILE
    }
}


