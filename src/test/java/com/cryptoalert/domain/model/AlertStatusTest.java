package com.cryptoalert.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AlertStatusTest {

    @Test
    void enumContainsExpectedValues() {
        assertEquals(3, AlertStatus.values().length);
        assertNotNull(AlertStatus.ACTIVE);
        assertNotNull(AlertStatus.TRIGGERED);
        assertNotNull(AlertStatus.CANCELLED);
    }
}
