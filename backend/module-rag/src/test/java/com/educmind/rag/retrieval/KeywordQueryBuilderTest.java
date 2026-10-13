package com.educmind.rag.retrieval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordQueryBuilderTest {

    @Test
    void cauHoiTiengViet_tachTuNoiBangOr() {
        assertEquals("bộ | nhớ | trong | là | gì", KeywordQueryBuilder.build("Bộ nhớ trong là gì?", 20));
    }

    @Test
    void chuThuongVaBoTrung() {
        assertEquals("bit", KeywordQueryBuilder.build("Bit bit BIT", 20));
    }

    @Test
    void khongCoTuDungDuoc_traRong() {
        assertEquals("", KeywordQueryBuilder.build("  ?! ... ", 20));
        assertEquals("", KeywordQueryBuilder.build(null, 20));
    }

    @Test
    void gioiHanSoTu() {
        assertEquals("aa | bb | cc", KeywordQueryBuilder.build("aa bb cc dd ee", 3));
    }

    @Test
    void chuanHoaNfc() {
        assertEquals("caf\u00e9", KeywordQueryBuilder.build("CAFE\u0301", 20));
    }

    @Test
    void kyTuDacBietCuaTsquery_khongLot()  {
        assertEquals("drop | table", KeywordQueryBuilder.build("drop table; x | y & !(z) :*", 20));
    }
}
