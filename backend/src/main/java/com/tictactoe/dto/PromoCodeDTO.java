package com.tictactoe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromoCodeDTO {
    private Long id;
    private String code;
    private Long gameId;
    private boolean used;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;
    private Integer discountPercent;

    // Для фронтенда
    private String formattedCreatedAt;
    private String formattedUsedAt;
    private String status; // "ACTIVE" или "USED"
}