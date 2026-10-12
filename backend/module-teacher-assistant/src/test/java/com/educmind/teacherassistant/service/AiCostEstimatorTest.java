package com.educmind.teacherassistant.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiCostEstimatorTest {

    private final AiCostEstimator estimator =
            new AiCostEstimator(new BigDecimal("0.75"), new BigDecimal("3.75"));

    @Test
    void estimate_1000In1000Out() {
        assertEquals(0, new BigDecimal("0.004500").compareTo(estimator.estimate(1000, 1000)));
    }

    @Test
    void estimate_realCall260In1071Out_roundsToSixDecimals() {
        assertEquals(0, new BigDecimal("0.004211").compareTo(estimator.estimate(260, 1071)));
    }

    @Test
    void estimate_nullTokensTreatedAsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(estimator.estimate(null, null)));
    }
}
