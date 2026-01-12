package com.tictactoe.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

@Data
public class NewGameRequestDTO {

    private String playerId;

    @JsonSetter(nulls = Nulls.SKIP)
    private String telegramChatId;

    @Override
    public String toString() {
        return "NewGameRequestDTO{" +
               "playerId='" + playerId + '\'' +
               ", telegramChatId='" + telegramChatId + '\'' +
               '}';
    }
}