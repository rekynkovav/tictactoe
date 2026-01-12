package com.tictactoe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameResponseDTO {
    private boolean success;
    private String message;
    private GameDTO game;
    private ComputerMoveDTO computerMove;
    private String promoCode;

    @Data
    public static class ComputerMoveDTO {
        private Integer row;
        private Integer column;
        private String symbol;

        public ComputerMoveDTO(Integer row, Integer column, String symbol) {
            this.row = row;
            this.column = column;
            this.symbol = symbol;
        }
    }

    public static GameResponseDTO success(GameDTO game, String message) {
        GameResponseDTO response = new GameResponseDTO();
        response.setSuccess(true);
        response.setMessage(message);
        response.setGame(game);
        return response;
    }

    public static GameResponseDTO successWithComputerMove(GameDTO game, ComputerMoveDTO computerMove, String message) {
        GameResponseDTO response = new GameResponseDTO();
        response.setSuccess(true);
        response.setMessage(message);
        response.setGame(game);
        response.setComputerMove(computerMove);
        return response;
    }

    public static GameResponseDTO successWithPromoCode(GameDTO game, String promoCode) {
        GameResponseDTO response = new GameResponseDTO();
        response.setSuccess(true);
        response.setMessage("Поздравляем! Вы победили и получили промокод!");
        response.setGame(game);
        response.setPromoCode(promoCode);
        return response;
    }

    public static GameResponseDTO error(String message) {
        GameResponseDTO response = new GameResponseDTO();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}