package com.cryptoalert.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PriceRecordTest {

    @Test
    void createsRecordWithProvidedValues() {
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");

        PriceRecord record = new PriceRecord("BTC", new BigDecimal("50000"), timestamp);

        assertEquals("BTC", record.symbol());
        assertEquals(new BigDecimal("50000"), record.price());
        assertEquals(timestamp, record.timestamp());
        assertNotNull(record.toString());
    }
}
