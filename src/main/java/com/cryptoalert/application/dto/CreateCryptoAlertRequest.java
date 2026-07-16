package com.cryptoalert.application.dto;

import com.cryptoalert.domain.model.AlertCondition;

import java.math.BigDecimal;

public record CreateCryptoAlertRequest(
        String symbol,
        BigDecimal targetPrice,
        AlertCondition condition
) {
}
