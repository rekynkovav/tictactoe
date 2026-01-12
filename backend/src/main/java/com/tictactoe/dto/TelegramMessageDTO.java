package com.tictactoe.dto;

import lombok.Data;

@Data
public class TelegramMessageDTO {
    private String chatId;
    private String message;
    private MessageType type;

    public enum MessageType {
        WIN,
        LOSE,
        DRAW,
        NEW_PROMOCODE
    }
}