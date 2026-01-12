package com.tictactoe.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "games")
@Data
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_chat_id")
    private String telegramChatId;

    @Type(JsonType.class)
    @Column(name = "board", columnDefinition = "jsonb")
    private String[][] board;

    @Column(name = "current_player", nullable = false, length = 1)
    private String currentPlayer = "X";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameStatus status = GameStatus.IN_PROGRESS;

    @Column(name = "player_id", length = 100)
    private String playerId;

    @Column(name = "promo_code", length = 20)
    private String promoCode;

    @Column(name = "player_moves_count")
    private Integer playerMovesCount = 0;

    @Column(name = "computer_moves_count")
    private Integer computerMovesCount = 0;

    @Column(name = "winning_line")
    private String winningLine;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "game_duration_seconds")
    private Long gameDurationSeconds;

    public Game(String playerId) {
        this.playerId = playerId;
        initializeEmptyBoard();
    }

    // Метод для инициализации пустой доски
    public void initializeEmptyBoard() {
        this.board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                this.board[i][j] = "";
            }
        }
    }

    // Метод для проверки, является ли доска пустой
    @JsonProperty("isBoardEmpty")
    public boolean isBoardEmpty() {
        if (board == null) return true;
        for (String[] row : board) {
            for (String cell : row) {
                if (cell != null && !cell.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    // Метод для подсчета занятых клеток
    @JsonProperty("occupiedCellsCount")
    public int getOccupiedCellsCount() {
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

    // Перечисление статусов игры
    public enum GameStatus {
        IN_PROGRESS("В процессе"),
        PLAYER_WON("Игрок победил"),
        COMPUTER_WON("Компьютер победил"),
        DRAW("Ничья"),
        ABANDONED("Прервана");

        private final String description;

        GameStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Game ID: ").append(id).append("\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Current Player: ").append(currentPlayer).append("\n");
        sb.append("Board:\n");

        if (board != null) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    String cell = board[i][j];
                    sb.append(cell == null || cell.isEmpty() ? " " : cell).append(" ");
                    if (j < 2) sb.append("| ");
                }
                if (i < 2) sb.append("\n---------\n");
            }
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return id != null && id.equals(game.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}