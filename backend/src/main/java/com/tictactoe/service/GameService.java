package com.tictactoe.service;

import com.tictactoe.dto.GameDTO;
import com.tictactoe.dto.GameResponseDTO;
import com.tictactoe.dto.MoveRequestDTO;
import com.tictactoe.dto.NewGameRequestDTO;
import com.tictactoe.model.Game;
import com.tictactoe.model.PlayerTelegramLink;
import com.tictactoe.repository.GameRepository;
import com.tictactoe.repository.PlayerTelegramLinkRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final PromoCodeService promoCodeService;
    private final TelegramService telegramService;
    private final PlayerTelegramLinkRepository playerTelegramLinkRepository;

    // Выигрышные комбинации
    private static final int[][] WINNING_COMBINATIONS = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // горизонтали
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // вертикали
            {0, 4, 8}, {2, 4, 6}             // диагонали
    };

    /**
     * Создать новую игру
     */
    @Transactional
    public GameDTO createNewGame(NewGameRequestDTO request) {
        log.info("=== НАЧАЛО СОЗДАНИЯ ИГРЫ ===");
        log.info("Получен запрос NewGameRequestDTO: {}", request);

        Game game = new Game();
        String playerId = request.getPlayerId() != null ? request.getPlayerId() : generatePlayerId();
        game.setPlayerId(playerId);

        String telegramChatId = determineTelegramChatId(request, playerId);

        if (telegramChatId != null) {
            game.setTelegramChatId(telegramChatId);
            log.info("✅ Установлен telegramChatId: '{}'", telegramChatId);
        } else {
            log.warn("⚠️ telegramChatId НЕ УСТАНОВЛЕН");
            game.setTelegramChatId(null);
        }

        game.setStatus(Game.GameStatus.IN_PROGRESS);
        game.setCurrentPlayer("X");
        game.initializeEmptyBoard();

        game = gameRepository.save(game);
        log.info("=== ИГРА СОЗДАНА ===");
        log.info("ID игры: {}, playerId: {}, telegramChatId: '{}'",
                game.getId(), game.getPlayerId(), game.getTelegramChatId());

        return GameDTO.fromEntity(game);
    }

    /**
     * Автоматически определить telegramChatId
     */
    private String determineTelegramChatId(NewGameRequestDTO request, String playerId) {
        String telegramChatId = request.getTelegramChatId();

        if (telegramChatId != null &&
            !telegramChatId.trim().isEmpty() &&
            !telegramChatId.trim().equalsIgnoreCase("null")) {
            // Сохраняем связь для будущего использования
            savePlayerTelegramLink(playerId, telegramChatId);
            return telegramChatId.trim();
        }

        // 2. Ищем по playerId в базе данных
        Optional<PlayerTelegramLink> link = playerTelegramLinkRepository.findByPlayerId(playerId);
        if (link.isPresent()) {
            return link.get().getTelegramChatId();
        }

        // 3. Не нашли - возвращаем null
        return null;
    }

    private void savePlayerTelegramLink(String playerId, String telegramChatId) {
        Optional<PlayerTelegramLink> existingLink = playerTelegramLinkRepository.findByPlayerId(playerId);

        if (existingLink.isPresent()) {
            PlayerTelegramLink link = existingLink.get();
            link.setTelegramChatId(telegramChatId);
            link.setLastUpdated(LocalDateTime.now());
            playerTelegramLinkRepository.save(link);
        } else {
            PlayerTelegramLink newLink = new PlayerTelegramLink();
            newLink.setPlayerId(playerId);
            newLink.setTelegramChatId(telegramChatId);
            newLink.setCreatedAt(LocalDateTime.now());
            newLink.setLastUpdated(LocalDateTime.now());
            playerTelegramLinkRepository.save(newLink);
        }
    }

    /**
     * Сделать ход игрока
     */
    @Transactional
    public GameResponseDTO makePlayerMove(MoveRequestDTO moveRequest) {
        Optional<Game> gameOpt = gameRepository.findById(moveRequest.getGameId());
        if (gameOpt.isEmpty()) {
            return GameResponseDTO.error("Игра не найдена");
        }

        Game game = gameOpt.get();

        if (game.getStatus() != Game.GameStatus.IN_PROGRESS) {
            return GameResponseDTO.error("Игра уже завершена");
        }

        if (!"X".equals(game.getCurrentPlayer())) {
            return GameResponseDTO.error("Сейчас не ваш ход");
        }

        String[][] board = game.getBoard();
        int row = moveRequest.getRow();
        int col = moveRequest.getColumn();

        if (row < 0 || row > 2 || col < 0 || col > 2) {
            return GameResponseDTO.error("Неверные координаты клетки");
        }

        if (board[row][col] != null && !board[row][col].isEmpty()) {
            return GameResponseDTO.error("Клетка уже занята");
        }

        board[row][col] = "X";
        game.setBoard(board);

        if (checkWin(board, "X")) {
            game.setStatus(Game.GameStatus.PLAYER_WON);
            game.setFinishedAt(LocalDateTime.now());

            String promoCode = promoCodeService.generateAndSavePromoCode(game);
            game.setPromoCode(promoCode);

            gameRepository.save(game);

            if (game.getTelegramChatId() != null && !game.getTelegramChatId().isEmpty()) {
                telegramService.sendWinNotification(game.getTelegramChatId(), promoCode);
            } else {
                log.warn("Не отправлено уведомление о победе: chatId не указан для игры ID: {}", game.getId());
            }

            log.info("Игрок победил в игре ID: {}, промокод: {}", game.getId(), promoCode);
            return GameResponseDTO.successWithPromoCode(GameDTO.fromEntity(game), promoCode);
        }

        if (isBoardFull(board)) {
            game.setStatus(Game.GameStatus.DRAW);
            game.setFinishedAt(LocalDateTime.now());
            gameRepository.save(game);

            if (game.getTelegramChatId() != null && !game.getTelegramChatId().isEmpty()) {
                telegramService.sendDrawNotification(game.getTelegramChatId());
            } else {
                log.warn("Не отправлено уведомление о ничье: chatId не указан для игры ID: {}", game.getId());
            }

            log.info("Ничья в игре ID: {}", game.getId());
            return GameResponseDTO.success(GameDTO.fromEntity(game), "Ничья!");
        }

        game.setCurrentPlayer("O");
        gameRepository.save(game);

        GameResponseDTO.ComputerMoveDTO computerMove = makeComputerMove(game);

        return GameResponseDTO.successWithComputerMove(
                GameDTO.fromEntity(game),
                computerMove,
                "Ход сделан. Компьютер сделал свой ход."
        );
    }

    /**
     * Ход компьютера
     */
    private GameResponseDTO.ComputerMoveDTO makeComputerMove(Game game) {
        String[][] board = game.getBoard();

        Integer[] winMove = findWinningMove(board, "O");
        if (winMove != null) {
            board[winMove[0]][winMove[1]] = "O";
            game.setBoard(board);
            game.setCurrentPlayer("X");
            game.setStatus(Game.GameStatus.COMPUTER_WON);
            game.setFinishedAt(LocalDateTime.now());
            gameRepository.save(game);

            if (game.getTelegramChatId() != null && !game.getTelegramChatId().isEmpty()) {
                telegramService.sendLoseNotification(game.getTelegramChatId());
            } else {
                log.warn("Не отправлено уведомление о проигрыше: chatId не указан для игры ID: {}", game.getId());
            }

            log.info("Компьютер победил в игре ID: {}", game.getId());
            return new GameResponseDTO.ComputerMoveDTO(winMove[0], winMove[1], "O");
        }

        Integer[] blockMove = findWinningMove(board, "X");
        if (blockMove != null) {
            board[blockMove[0]][blockMove[1]] = "O";
            game.setBoard(board);
            game.setCurrentPlayer("X");
            gameRepository.save(game);
            return new GameResponseDTO.ComputerMoveDTO(blockMove[0], blockMove[1], "O");
        }

        if (board[1][1] == null || board[1][1].isEmpty()) {
            board[1][1] = "O";
            game.setBoard(board);
            game.setCurrentPlayer("X");
            gameRepository.save(game);
            return new GameResponseDTO.ComputerMoveDTO(1, 1, "O");
        }

        int[][] corners = {{0, 0}, {0, 2}, {2, 0}, {2, 2}};
        for (int[] corner : corners) {
            if (board[corner[0]][corner[1]] == null || board[corner[0]][corner[1]].isEmpty()) {
                board[corner[0]][corner[1]] = "O";
                game.setBoard(board);
                game.setCurrentPlayer("X");
                gameRepository.save(game);
                return new GameResponseDTO.ComputerMoveDTO(corner[0], corner[1], "O");
            }
        }

        Random random = new Random();
        int row, col;
        do {
            row = random.nextInt(3);
            col = random.nextInt(3);
        } while (board[row][col] != null && !board[row][col].isEmpty());

        board[row][col] = "O";
        game.setBoard(board);
        game.setCurrentPlayer("X");
        gameRepository.save(game);

        return new GameResponseDTO.ComputerMoveDTO(row, col, "O");
    }

    /**
     * Найти выигрышный ход для указанного символа
     */
    private Integer[] findWinningMove(String[][] board, String symbol) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == null || board[i][j].isEmpty()) {
                    board[i][j] = symbol;
                    if (checkWin(board, symbol)) {
                        board[i][j] = null;
                        return new Integer[]{i, j};
                    }
                    board[i][j] = "";
                }
            }
        }
        return null;
    }

    /**
     * Проверка на победу
     */
    private boolean checkWin(String[][] board, String symbol) {
        String[] flatBoard = new String[9];
        int index = 0;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                flatBoard[index++] = board[i][j];
            }
        }

        for (int[] combination : WINNING_COMBINATIONS) {
            if (flatBoard[combination[0]] != null && flatBoard[combination[0]].equals(symbol) &&
                flatBoard[combination[1]] != null && flatBoard[combination[1]].equals(symbol) &&
                flatBoard[combination[2]] != null && flatBoard[combination[2]].equals(symbol)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Проверка на заполненность доски
     */
    private boolean isBoardFull(String[][] board) {
        for (String[] row : board) {
            for (String cell : row) {
                if (cell == null || cell.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Получить игру по ID
     */
    public GameDTO getGameById(Long gameId) {
        return gameRepository.findById(gameId)
                .map(GameDTO::fromEntity)
                .orElse(null);
    }

    /**
     * Получить все игры игрока
     */
    public java.util.List<GameDTO> getPlayerGames(String playerId) {
        return gameRepository.findByPlayerId(playerId).stream()
                .map(GameDTO::fromEntity)
                .toList();
    }

    /**
     * Генерация ID игрока (упрощенная версия)
     */
    private String generatePlayerId() {
        return "player_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
    }
}