package com.tictactoe.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveRequestDTO {
    @NotNull
    private Long gameId;

    @NotNull
    private String playerId;

    @NotNull
    private int row;

    @NotNull
    private int column;
}