package com.tictactoe.controller;

import com.tictactoe.model.Game;
import com.tictactoe.model.PlayerTelegramLink;
import com.tictactoe.repository.GameRepository;
import com.tictactoe.repository.PlayerTelegramLinkRepository;
import com.tictactoe.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramController {

    private final TelegramService telegramService;
    private final GameRepository gameRepository;
    private final PlayerTelegramLinkRepository playerTelegramLinkRepository;

    /**
     * Проверить статус бота
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBotStatus() {
        log.info("Запрос на проверку статуса бота");

        Map<String, Object> response = new HashMap<>();
        boolean isConnected = telegramService.testConnection();

        response.put("status", isConnected ? "ONLINE" : "OFFLINE");
        response.put("botName", telegramService.getBotUsername());
        response.put("connected", isConnected);
        response.put("message", isConnected ?
                "✅ Telegram бот активен и готов к работе!" :
                "❌ Telegram бот недоступен");

        log.info("Статус бота: {}", isConnected ? "ONLINE" : "OFFLINE");
        return ResponseEntity.ok(response);
    }

    /**
     * Получить или создать связь playerId <-> telegramChatId
     */
    @PostMapping("/link")
    public ResponseEntity<Map<String, Object>> createOrUpdateLink(
            @RequestBody Map<String, String> request) {

        String playerId = request.get("playerId");
        String telegramChatId = request.get("telegramChatId");

        log.info("Создание/обновление связи: playerId={}, telegramChatId={}",
                playerId, telegramChatId);

        if (playerId == null || telegramChatId == null) {
            throw new IllegalArgumentException("playerId и telegramChatId обязательны");
        }

        // Сохраняем связь
        Optional<PlayerTelegramLink> existingLink = playerTelegramLinkRepository.findByPlayerId(playerId);

        PlayerTelegramLink link;
        if (existingLink.isPresent()) {
            link = existingLink.get();
            link.setTelegramChatId(telegramChatId);
            link.setLastUpdated(LocalDateTime.now());
        } else {
            link = new PlayerTelegramLink();
            link.setPlayerId(playerId);
            link.setTelegramChatId(telegramChatId);
            link.setCreatedAt(LocalDateTime.now());
            link.setLastUpdated(LocalDateTime.now());
        }

        playerTelegramLinkRepository.save(link);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Связь установлена");
        response.put("playerId", playerId);
        response.put("telegramChatId", telegramChatId);

        return ResponseEntity.ok(response);
    }

    /**
     * Проверить связь для игрока
     */
    @GetMapping("/link/{playerId}")
    public ResponseEntity<Map<String, Object>> getLink(@PathVariable String playerId) {
        Optional<PlayerTelegramLink> link = playerTelegramLinkRepository.findByPlayerId(playerId);

        Map<String, Object> response = new HashMap<>();
        response.put("playerId", playerId);
        response.put("hasTelegramLink", link.isPresent());

        if (link.isPresent()) {
            response.put("telegramChatId", link.get().getTelegramChatId());
            response.put("createdAt", link.get().getCreatedAt());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Получить Chat ID для игрока
     */
    @GetMapping("/player/{playerId}/chatid")
    public ResponseEntity<Map<String, Object>> getPlayerChatId(@PathVariable String playerId) {
        log.info("Получен запрос Chat ID для playerId: {}", playerId);

        List<Game> games = gameRepository.findByPlayerId(playerId);
        String chatId = games.stream()
                .filter(g -> g.getTelegramChatId() != null && !g.getTelegramChatId().isEmpty())
                .map(Game::getTelegramChatId)
                .findFirst()
                .orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("playerId", playerId);
        response.put("chatId", chatId);
        response.put("hasChatId", chatId != null);
        response.put("totalGames", games.size());

        return ResponseEntity.ok(response);
    }
}