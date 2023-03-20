package com.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostMessage {

    private Status messageStatus;
    private String message;


    public static enum Status {
        NEW, DONE
    }
}


