package com.educmind.rag.retrieval;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Hằng số truy xuất, đọc từ educmind.rag.retrieval.* (có giá trị mặc định). */
@Component
public class RetrievalSettings {

    private final int vectorCandidates;
    private final int keywordCandidates;
    private final int topK;
    private final int rrfK;
    private final int keywordMaxTokens;

    public RetrievalSettings(
            @Value("${educmind.rag.retrieval.vector-candidates:20}") int vectorCandidates,
            @Value("${educmind.rag.retrieval.keyword-candidates:20}") int keywordCandidates,
            @Value("${educmind.rag.retrieval.top-k:5}") int topK,
            @Value("${educmind.rag.retrieval.rrf-k:60}") int rrfK,
            @Value("${educmind.rag.retrieval.keyword-max-tokens:20}") int keywordMaxTokens) {
        this.vectorCandidates = vectorCandidates;
        this.keywordCandidates = keywordCandidates;
        this.topK = topK;
        this.rrfK = rrfK;
        this.keywordMaxTokens = keywordMaxTokens;
    }

    public int vectorCandidates() {
        return vectorCandidates;
    }

    public int keywordCandidates() {
        return keywordCandidates;
    }

    public int topK() {
        return topK;
    }

    public int rrfK() {
        return rrfK;
    }

    public int keywordMaxTokens() {
        return keywordMaxTokens;
    }
}
