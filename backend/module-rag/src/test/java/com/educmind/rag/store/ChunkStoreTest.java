package com.educmind.rag.store;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkStoreTest {

    @Test
    void toVectorLiteral_formatsAsPgvectorArray() {
        assertEquals("[0.5,-1.0,0.25]", ChunkStore.toVectorLiteral(new float[]{0.5f, -1.0f, 0.25f}));
    }

    @Test
    void toVectorLiteral_singleElement() {
        assertEquals("[1.0]", ChunkStore.toVectorLiteral(new float[]{1.0f}));
    }
}
