package com.tictactoe.repository;

import com.tictactoe.model.PlayerTelegramLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlayerTelegramLinkRepository extends JpaRepository<PlayerTelegramLink, Long> {
    Optional<PlayerTelegramLink> findByPlayerId(String playerId);
    Optional<PlayerTelegramLink> findByTelegramChatId(String telegramChatId);
}