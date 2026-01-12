package com.tictactoe.service;

import com.tictactoe.model.Game;
import com.tictactoe.model.PromoCode;
import com.tictactoe.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    @Value("${app.promo-code.length:5}")
    private int promoCodeLength;

    @Value("${app.promo-code.charset:ABCDEFGHJKLMNPQRSTUVWXYZ23456789}")
    private String charset;

    private static final SecureRandom random = new SecureRandom();

    /**
     * Генерация и сохранение промокода для игры
     */
    public String generateAndSavePromoCode(Game game) {
        String code = generateUniquePromoCode();

        PromoCode promoCode = new PromoCode();
        promoCode.setCode(code);
        promoCode.setGame(game);
        promoCode.setDiscountPercent(15); // Стандартная скидка 15%
        promoCode.setUsed(false);
        promoCode.setCreatedAt(LocalDateTime.now());

        promoCodeRepository.save(promoCode);
        log.info("Сгенерирован промокод: {} для игры ID: {}", code, game.getId());

        return code;
    }

    /**
     * Генерация уникального промокода
     */
    private String generateUniquePromoCode() {
        String code;
        do {
            code = generateRandomCode();
        } while (promoCodeRepository.existsByCode(code));

        return code;
    }

    /**
     * Генерация случайного кода
     */
    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(promoCodeLength);
        for (int i = 0; i < promoCodeLength; i++) {
            int index = random.nextInt(charset.length());
            sb.append(charset.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Проверка валидности промокода
     */
    public boolean isValidPromoCode(String code) {
        return promoCodeRepository.findByCode(code)
                .map(promo -> !promo.isUsed())
                .orElse(false);
    }

    /**
     * Использовать промокод
     */
    public boolean usePromoCode(String code) {
        return promoCodeRepository.findByCode(code)
                .map(promo -> {
                    if (!promo.isUsed()) {
                        promo.setUsed(true);
                        promo.setUsedAt(LocalDateTime.now());
                        promoCodeRepository.save(promo);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Получить информацию о промокоде
     */
    public PromoCode getPromoCodeInfo(String code) {
        return promoCodeRepository.findByCode(code).orElse(null);
    }
}