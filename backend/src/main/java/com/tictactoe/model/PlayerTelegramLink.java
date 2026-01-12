package com.tictactoe.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_telegram_links")
@Data
public class PlayerTelegramLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String playerId;

    @Column(nullable = false)
    private String telegramChatId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastUpdated;
}