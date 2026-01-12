package com.tictactoe.controller;

import com.tictactoe.dto.PromoCodeDTO;
import com.tictactoe.model.PromoCode;
import com.tictactoe.service.PromoCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promo-codes")
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    /**
     * Проверить промокод
     */
    @GetMapping("/check/{code}")
    public ResponseEntity<Boolean> checkPromoCode(@PathVariable String code) {
        boolean isValid = promoCodeService.isValidPromoCode(code);
        return ResponseEntity.ok(isValid);
    }

    /**
     * Использовать промокод
     */
    @PostMapping("/use/{code}")
    public ResponseEntity<Boolean> usePromoCode(@PathVariable String code) {
        boolean used = promoCodeService.usePromoCode(code);
        return ResponseEntity.ok(used);
    }

    /**
     * Получить информацию о промокоде
     */
    @GetMapping("/{code}")
    public ResponseEntity<PromoCodeDTO> getPromoCodeInfo(@PathVariable String code) {
        PromoCode promoCode = promoCodeService.getPromoCodeInfo(code);
        if (promoCode == null) {
            return ResponseEntity.notFound().build();
        }

        PromoCodeDTO dto = new PromoCodeDTO();
        dto.setId(promoCode.getId());
        dto.setCode(promoCode.getCode());
        dto.setGameId(promoCode.getGame() != null ? promoCode.getGame().getId() : null);
        dto.setUsed(promoCode.isUsed());
        dto.setCreatedAt(promoCode.getCreatedAt());
        dto.setUsedAt(promoCode.getUsedAt());
        dto.setDiscountPercent(promoCode.getDiscountPercent());

        return ResponseEntity.ok(dto);
    }
}