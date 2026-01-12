package com.tictactoe.repository;

import com.tictactoe.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByPlayerId(String playerId);

    List<Game> findByTelegramChatId(String telegramChatId);

}