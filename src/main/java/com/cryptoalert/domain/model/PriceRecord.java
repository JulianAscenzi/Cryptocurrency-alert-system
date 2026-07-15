package com.cryptoalert.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable value object representing a price update for a specific cryptocurrency symbol at a point in time.
 */
public record PriceRecord(
    String symbol,
    BigDecimal price,
    Instant timestamp
) {}
