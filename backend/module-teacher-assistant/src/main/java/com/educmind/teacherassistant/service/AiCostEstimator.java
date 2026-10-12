package com.educmind.teacherassistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ước tính chi phí một lượt gọi AI theo số token (USD), giá lấy từ cấu hình để đổi được khi nhà cung cấp đổi giá.
 */
@Component
public class AiCostEstimator {

    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    private final BigDecimal priceInputPerMillion;
    private final BigDecimal priceOutputPerMillion;

    public AiCostEstimator(
            @Value("${educmind.ai.gemini.price-input-per-million:0.75}") BigDecimal priceInputPerMillion,
            @Value("${educmind.ai.gemini.price-output-per-million:3.75}") BigDecimal priceOutputPerMillion) {
        this.priceInputPerMillion = priceInputPerMillion;
        this.priceOutputPerMillion = priceOutputPerMillion;
    }

    public BigDecimal estimate(Integer inputTokens, Integer outputTokens) {
        long in = inputTokens == null ? 0 : inputTokens;
        long out = outputTokens == null ? 0 : outputTokens;
        return BigDecimal.valueOf(in).multiply(priceInputPerMillion)
                .add(BigDecimal.valueOf(out).multiply(priceOutputPerMillion))
                .divide(MILLION, 6, RoundingMode.HALF_UP);
    }
}
