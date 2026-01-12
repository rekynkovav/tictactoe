package com.tictactoe.controller;

import com.tictactoe.dto.GameDTO;
import com.tictactoe.dto.GameResponseDTO;
import com.tictactoe.dto.MoveRequestDTO;
import com.tictactoe.dto.NewGameRequestDTO;
import com.tictactoe.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;

    /**
     * Создать новую игру
     */
    @PostMapping("/new")
    public ResponseEntity<GameDTO> createNewGame(@RequestBody @Valid NewGameRequestDTO request) {
        log.info("📨 Получен POST запрос на /api/games/new");
        log.info("playerId: {}, telegramChatId: {}",
                request.getPlayerId(),
                request.getTelegramChatId() != null ? "'" + request.getTelegramChatId() + "'" : "null");

        if (request.getTelegramChatId() != null) {
            log.info("📱 Telegram уведомления включены для chatId: {}", request.getTelegramChatId());
        } else {
            log.warn("⚠️ Telegram уведомления отключены (chatId не указан)");
        }

        GameDTO game = gameService.createNewGame(request);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/move")
    public ResponseEntity<GameResponseDTO> makeMove(@RequestBody @Valid MoveRequestDTO moveRequest) {
        GameResponseDTO response = gameService.makePlayerMove(moveRequest);
        return ResponseEntity.ok(response);
    }


    /**
     * Получить информацию об игре
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameDTO> getGame(@PathVariable Long gameId) {
        GameDTO game = gameService.getGameById(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(game);
    }

    /**
     * Получить все игры игрока
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<GameDTO>> getPlayerGames(@PathVariable String playerId) {
        List<GameDTO> games = gameService.getPlayerGames(playerId);
        return ResponseEntity.ok(games);
    }

    @PostMapping("/test-new")
    public ResponseEntity<Map<String, Object>> createTestGame(
            @RequestBody Map<String, String> request) {

        log.info("Тестовый запрос: {}", request);

        NewGameRequestDTO dto = new NewGameRequestDTO();
        dto.setPlayerId(request.get("playerId"));
        dto.setTelegramChatId(request.get("telegramChatId"));

        GameDTO game = gameService.createNewGame(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("game", game);
        response.put("receivedData", request);

        return ResponseEntity.ok(response);
    }
}