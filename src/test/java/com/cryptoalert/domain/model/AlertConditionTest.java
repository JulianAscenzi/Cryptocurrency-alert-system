package com.cryptoalert.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AlertConditionTest {

    @Test
    void enumContainsExpectedValues() {
        assertEquals(2, AlertCondition.values().length);
        assertNotNull(AlertCondition.ABOVE);
        assertNotNull(AlertCondition.BELOW);
    }
}
