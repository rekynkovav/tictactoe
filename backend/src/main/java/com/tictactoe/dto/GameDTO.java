package com.tictactoe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tictactoe.model.Game;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameDTO {
    private Long id;
    private String[][] board;
    private String currentPlayer;
    private String status;
    private String playerId;
    private String promoCode;
    private String createdAt;
    private String finishedAt;
    private Boolean isFinished;

    private Integer occupiedCells;
    private Boolean isPlayerTurn;
    private String winner;

    public static GameDTO fromEntity(Game game) {
        GameDTO dto = new GameDTO();
        dto.setId(game.getId());
        dto.setBoard(game.getBoard());
        dto.setCurrentPlayer(game.getCurrentPlayer());
        dto.setStatus(game.getStatus().name());
        dto.setPlayerId(game.getPlayerId());
        dto.setPromoCode(game.getPromoCode());

        // Форматируем даты
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (game.getCreatedAt() != null) {
            dto.setCreatedAt(game.getCreatedAt().format(formatter));
        }
        if (game.getFinishedAt() != null) {
            dto.setFinishedAt(game.getFinishedAt().format(formatter));
        }

        // Вычисляем дополнительные поля
        dto.setIsFinished(game.getStatus() != Game.GameStatus.IN_PROGRESS);
        dto.setOccupiedCells(countOccupiedCells(game.getBoard()));
        dto.setIsPlayerTurn("X".equals(game.getCurrentPlayer()));

        // Определяем победителя
        if (game.getStatus() == Game.GameStatus.PLAYER_WON) {
            dto.setWinner("PLAYER");
        } else if (game.getStatus() == Game.GameStatus.COMPUTER_WON) {
            dto.setWinner("COMPUTER");
        }

        return dto;
    }

    private static Integer countOccupiedCells(String[][] board) {
        if (board == null) return 0;
        int count = 0;
        for (String[] row : board) {
            for (String cell : row) {
                if (cell != null && !cell.trim().isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }
}