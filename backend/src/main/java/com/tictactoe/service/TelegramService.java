package com.tictactoe.service;

import com.tictactoe.model.Game;
import com.tictactoe.model.PlayerTelegramLink;
import com.tictactoe.model.PromoCode;
import com.tictactoe.repository.GameRepository;
import com.tictactoe.repository.PlayerTelegramLinkRepository;
import com.tictactoe.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService extends TelegramLongPollingBot {

    private final PlayerTelegramLinkRepository playerTelegramLinkRepository;
    private final GameRepository gameRepository;
    private final PromoCodeRepository promoCodeRepository;

    private String botToken = "8520423880:AAEr5qID0DsjFP82kyVzIgb7dzpZpnZrNbI";

    private String botUsername = "TicTacToePrizeBot";

    @Value("${app.game-url:http://localhost}")
    private String gameUrl;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            log.info("Получено сообщение от {}: {}", chatId, messageText);

            switch (messageText) {
                case "/start":
                    sendWelcomeMessage(chatId);
                    break;
                case "/promo":
                    sendPromoInfo(chatId);
                    break;
                case "/game":
                    sendGameInfo(chatId);
                    break;
                case "/ссылка":
                    sendGameLink(chatId);
                    break;
                default:
                    sendDefaultResponse(chatId);
            }
        }
    }

    /**
     * Отправка приветственного сообщения
     */
    private void sendWelcomeMessage(String chatId) {
        log.info("📨 Обработка команды /start для chatId: {}", chatId);

        // 1. Генерируем уникальный playerId для этого пользователя
        String playerId = "player_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
        log.info("✅ Сгенерирован playerId: {}", playerId);

        // 2. Сохраняем связь playerId <-> chatId
        savePlayerTelegramLink(playerId, chatId);
        log.info("✅ Сохранена связь: playerId={} -> chatId={}", playerId, chatId);

        // 3. Формируем ссылку с параметрами
        String gameBaseUrl = gameUrl;

        // Убедимся, что URL правильный
        if (!gameBaseUrl.startsWith("http")) {
            gameBaseUrl = "http://" + gameBaseUrl;
        }

        // Формируем полный URL с параметрами
        String gameUrlWithParams = gameBaseUrl + "/?playerId=" + playerId + "&telegramChatId=" + chatId;

        log.info("✅ Сформирована ссылка: {}", gameUrlWithParams);

        // 4. Формируем сообщение со ссылкой
        String message = "🎮 *Добро пожаловать в игру Крестики-нолики!*\n\n" +
                         "🎯 *Ваш Player ID:* `" + playerId + "`\n" +
                         "🎯 *Ваш Chat ID:* `" + chatId + "`\n\n" +
                         "🎮 *Чтобы начать играть:*\n" +
                         "Нажмите на ссылку ниже:\n\n" +
                         "👉 " + gameUrlWithParams + "\n\n" +
                         "*Или скопируйте и вставьте в браузер:*\n" +
                         "```\n" + gameUrlWithParams + "\n```\n\n" +
                         "📋 *После перехода по ссылке:*\n" +
                         "✅ Ваши данные подставятся автоматически\n" +
                         "✅ Начните играть и побеждать!\n" +
                         "✅ При победе получите промокод в этом чате!\n\n" +
                         "💡 *Совет:* Сохраните эту ссылку для быстрого доступа к игре!";

        // 5. Отправляем сообщение
        sendMessage(chatId, message);
        log.info("✅ Сообщение отправлено пользователю chatId: {}", chatId);
    }

    /**
     * Отправить ссылку на игру
     */
    public void sendGameLink(String chatId) {
        String message = "🎮 *Ссылка на игру*\n\n" +
                         "Нажмите на ссылку ниже, чтобы начать играть:\n\n" +
                         "🔗 " + gameUrl + "\n\n" +
                         "⬇️ *Инструкция:*\n" +
                         "1. Откройте ссылку\n" +
                         "2. Нажмите 'Новая игра'\n" +
                         "3. Делайте ходы, кликая по клеткам\n" +
                         "4. Получайте промокоды!";

        sendMessage(chatId, message);
    }

    /**
     * Информация о промокодах
     */
    private void sendPromoInfo(String chatId) {
        try {
            List<Game> games = gameRepository.findByTelegramChatId(chatId);
            Optional<Game> lastGame = games.stream().findFirst();

            String message = "💎 *Информация о промокодах*\n\n";

            if (lastGame.isPresent()) {
                Game game = lastGame.get();
                Optional<PromoCode> promoOpt = promoCodeRepository.findByGameId(game.getId());

                if (promoOpt.isPresent()) {
                    PromoCode promo = promoOpt.get();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

                    message += "🎁 *Последний промокод:*\n" +
                               "🔑 *Код:* `" + promo.getCode() + "`\n" +
                               "💰 *Скидка:* " + promo.getDiscountPercent() + "%\n" +
                               "📅 *Создан:* " + promo.getCreatedAt().format(formatter) + "\n" +
                               "✅ *Статус:* " + (promo.isUsed() ? "Использован" : "Активен") + "\n\n";

                    if (promo.isUsed() && promo.getUsedAt() != null) {
                        message += "🕐 *Использован:* " + promo.getUsedAt().format(formatter) + "\n\n";
                    }
                } else {
                    message += "У вас пока нет промокодов.\n" +
                               "🎮 *Сыграйте и выиграйте, чтобы получить первый промокод!*\n\n";
                }
            } else {
                message += "У вас пока нет промокодов.\n" +
                           "🎮 *Сыграйте и выиграйте, чтобы получить первый промокод!*\n\n";
            }

            message += "📋 *Правила промокодов:*\n" +
                       "• Действуют 30 дней с момента получения\n" +
                       "• Скидка 15% на следующую покупку\n" +
                       "• Один промокод = одна скидка\n" +
                       "• Не передавайте коды другим\n\n" +
                       "❓ *Как использовать:*\n" +
                       "1. Скопируйте промокод\n" +
                       "2. Введите при оформлении заказа\n" +
                       "3. Наслаждайтесь скидкой!";

            sendMessage(chatId, message);
        } catch (Exception e) {
            log.error("Ошибка получения информации о промокодах: {}", e.getMessage());
            sendMessage(chatId, "⚠️ Не удалось получить информацию о промокодах.");
        }
    }

    /**
     * Информация о последней игре
     */
    private void sendGameInfo(String chatId) {
        try {
            List<Game> games = gameRepository.findByTelegramChatId(chatId);
            Optional<Game> lastGame = games.stream().findFirst();

            String message = "🎮 *Информация об игре*\n\n";

            if (lastGame.isPresent()) {
                Game game = lastGame.get();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

                message += "🆔 *ID игры:* " + game.getId() + "\n" +
                           "📅 *Начало:* " + game.getCreatedAt().format(formatter) + "\n" +
                           "🎯 *Статус:* " + game.getStatus().name() + "\n" +
                           "👤 *Ход:* " + ("X".equals(game.getCurrentPlayer()) ? "Игрок" : "Компьютер") + "\n\n";

                if (game.getFinishedAt() != null) {
                    message += "⏱️ *Завершена:* " + game.getFinishedAt().format(formatter) + "\n\n";
                }

                // Отображаем доску
                message += "🎲 *Доска:*\n";
                String[][] board = game.getBoard();
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        String cell = board[i][j];
                        if (cell == null || cell.isEmpty()) {
                            message += "⬜️";
                        } else if ("X".equals(cell)) {
                            message += "❌";
                        } else {
                            message += "⭕️";
                        }
                    }
                    message += "\n";
                }

                // Добавляем кнопку для игры
                message += "\n🎮 *Начать новую игру:*\n";
                message += "[Начать игру](" + gameUrl + ")";

            } else {
                message += "У вас пока нет сыгранных игр.\n" +
                           "🎮 *Начните игру прямо сейчас!*\n\n" +
                           "🔗 *Ссылка на игру:* " + gameUrl + "\n" +
                           "⬇️ *Просто нажмите ссылку выше*";
            }

            sendMessage(chatId, message);
        } catch (Exception e) {
            log.error("Ошибка получения информации об игре: {}", e.getMessage());
            sendMessage(chatId, "⚠️ Не удалось получить информацию об игре.\n\n" +
                                "🎮 *Ссылка на игру:* " + gameUrl);
        }
    }

    /**
     * Ответ по умолчанию
     */
    private void sendDefaultResponse(String chatId) {
        String message = "🤖 *Я не понимаю эту команду*\n\n" +
                         "Попробуйте одну из этих команд:\n" +
                         "/start - Приветствие и инструкция\n" +
                         "/help - Помощь\n" +
                         "/stats - Статистика\n" +
                         "/promo - Промокоды\n" +
                         "/game - Информация об игре\n" +
                         "/link - Получить ссылку на игру\n\n" +
                         "🔗 *Ссылка на игру:* " + gameUrl;

        sendMessage(chatId, message);
    }

    /**
     * Отправка уведомления о победе
     */
    public void sendWinNotification(String chatId, String promoCode) {
        if (chatId != null && !chatId.trim().isEmpty()) {
            String message = "Победа! Промокод выдан: " + promoCode + "*";

            sendMessage(chatId, message);
            log.info("Отправлено уведомление о победе в Telegram chatId: {}, промокод: {}", chatId, promoCode);
        } else {
            log.warn("Не удалось отправить уведомление о победе: chatId не указан");
        }
    }

    /**
     * Отправка уведомления о проигрыше
     */
    public void sendLoseNotification(String chatId) {
        if (chatId != null && !chatId.trim().isEmpty()) {
            String message = "Проигрыш";

            sendMessage(chatId, message);
            log.info("Отправлено уведомление о проигрыше в Telegram chatId: {}", chatId);
        } else {
            log.warn("Не удалось отправить уведомление о проигрыше: chatId не указан");
        }
    }

    /**
     * Отправка уведомления о ничье
     */
    public void sendDrawNotification(String chatId) {
        if (chatId != null && !chatId.trim().isEmpty()) {
            String message = "🤝 *НИЧЬЯ*\n\n" +
                             "Игра завершилась вничью.\n\n" +
                             "⚔️ *Равная борьба!*\n" +
                             "Вы были наравне с компьютером.\n\n" +
                             "🎮 *Сыграйте ещё раз — победитель определится в следующей игре!*\n" +
                             "[Начать новую игру](" + gameUrl + ")\n\n" +
                             "*Ничья*";

//            sendMessage(chatId, message);
            log.info("Отправлено уведомление о ничье в Telegram chatId: {}", chatId);
        } else {
            log.warn("Не удалось отправить уведомление о ничье: chatId не указан");
        }
    }

    /**
     * Проверка соединения
     */
    public boolean testConnection() {
        try {
            GetMe getMe = new GetMe();
            User botUser = execute(getMe);
            log.info("Telegram бот успешно подключен: @{}", botUser.getUserName());
            return true;
        } catch (TelegramApiException e) {
            log.error("Ошибка подключения Telegram бота: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Общий метод отправки сообщения
     */
    public void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown");

        try {
            execute(message);
            log.debug("Сообщение отправлено в Telegram chatId: {}, текст: {}", chatId,
                    text.length() > 50 ? text.substring(0, 50) + "..." : text);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения в Telegram chatId: {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Обработка команды /start
     */
    private void handleStartCommand(String chatId, Long userId) {
        // Генерируем playerId для этого пользователя
        String playerId = "player_" + userId + "_" + System.currentTimeMillis();

        // Сохраняем связь
        savePlayerTelegramLink(playerId, chatId);

        // Отправляем сообщение с ссылкой для игры
        String gameUrlWithParams = gameUrl + "?playerId=" + playerId + "&telegramChatId=" + chatId;

        String message = "🎮 *Добро пожаловать в игру Крестики-нолики!*\n\n" +
                         "🎯 *Ваш Player ID:* `" + playerId + "`\n" +
                         "🎯 *Ваш Chat ID:* `" + chatId + "`\n\n" +
                         "🎮 *Начать играть:*\n" +
                         "Просто нажмите на ссылку ниже и игра запустится автоматически!\n\n" +
                         "🔗 [Начать игру](" + gameUrlWithParams + ")\n\n" +
                         "📋 *После нажатия на ссылку:*\n" +
                         "1. Игра откроется в браузере\n" +
                         "2. Ваши данные подставятся автоматически\n" +
                         "3. Начните играть!\n\n" +
                         "💡 *Совет:* Сохраните эту ссылку для быстрого доступа к игре!";

        sendMessage(chatId, message);
        log.info("✅ Зарегистрирован новый игрок: playerId={}, chatId={}", playerId, chatId);
    }

    /**
     * Сохранить связь между playerId и telegramChatId
     */
    private void savePlayerTelegramLink(String playerId, String telegramChatId) {
        Optional<PlayerTelegramLink> existingLink = playerTelegramLinkRepository.findByPlayerId(playerId);

        if (existingLink.isPresent()) {
            // Обновляем существующую запись
            PlayerTelegramLink link = existingLink.get();
            link.setTelegramChatId(telegramChatId);
            link.setLastUpdated(LocalDateTime.now());
            playerTelegramLinkRepository.save(link);
            log.debug("Обновлена связь playerId {} -> telegramChatId {}", playerId, telegramChatId);
        } else {
            // Создаем новую запись
            PlayerTelegramLink newLink = new PlayerTelegramLink();
            newLink.setPlayerId(playerId);
            newLink.setTelegramChatId(telegramChatId);
            newLink.setCreatedAt(LocalDateTime.now());
            newLink.setLastUpdated(LocalDateTime.now());
            playerTelegramLinkRepository.save(newLink);
            log.debug("Создана связь playerId {} -> telegramChatId {}", playerId, telegramChatId);
        }
    }

    /**
     * Получить Chat ID по playerId
     */
    public String getTelegramChatIdByPlayerId(String playerId) {
        return playerTelegramLinkRepository.findByPlayerId(playerId)
                .map(PlayerTelegramLink::getTelegramChatId)
                .orElse(null);
    }

    /**
     * Получить Player ID по Chat ID
     */
    public String getPlayerIdByTelegramChatId(String telegramChatId) {
        return playerTelegramLinkRepository.findByTelegramChatId(telegramChatId)
                .map(PlayerTelegramLink::getPlayerId)
                .orElseGet(() -> {
                    // Если не найден, создаем новый
                    String newPlayerId = "player_" + System.currentTimeMillis();
                    savePlayerTelegramLink(newPlayerId, telegramChatId);
                    return newPlayerId;
                });
    }
}