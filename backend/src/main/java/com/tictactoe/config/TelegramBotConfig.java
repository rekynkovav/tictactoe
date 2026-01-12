package com.tictactoe.config;

import com.tictactoe.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TelegramBotConfig {

    private final TelegramService telegramService;

    @Bean
    public TelegramBotsApi telegramBotsApi() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramService);
            log.info("Telegram бот успешно зарегистрирован и запущен");
            telegramService.testConnection();
            return botsApi;
        } catch (TelegramApiException e) {
            log.error("Ошибка регистрации Telegram бота: {}", e.getMessage());
            throw new RuntimeException("Не удалось зарегистрировать Telegram бота", e);
        }
    }
}