package com.educmind.rag.retrieval;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reciprocal Rank Fusion: gộp nhiều danh sách đã xếp hạng thành một.
 * Điểm của mỗi phần tử = tổng 1 / (k + hạng) trên các danh sách chứa nó (hạng bắt đầu từ 1).
 */
public final class RrfFuser {

    public record Scored<K>(K key, double score) {
    }

    private RrfFuser() {
    }

    public static <K> List<Scored<K>> fuse(List<? extends List<K>> rankedLists, int k, int limit) {
        if (k <= 0) {
            throw new IllegalArgumentException("k phải lớn hơn 0");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit phải lớn hơn 0");
        }
        Map<K, Double> scores = new LinkedHashMap<>();
        for (List<K> list : rankedLists) {
            Set<K> seen = new HashSet<>();
            int rank = 0;
            for (K key : list) {
                if (!seen.add(key)) {
                    continue;
                }
                rank++;
                scores.merge(key, 1.0 / (k + rank), Double::sum);
            }
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<K, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> new Scored<>(e.getKey(), e.getValue()))
                .toList();
    }
}
