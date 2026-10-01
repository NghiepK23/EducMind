package com.educmind.common.pagination;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Chuan 1 (offset) - CrossModule_PATCH3_Pagination: page 1-indexed (mac dinh 1),
 * size mac dinh 20, toi da 100 (vuot thi ke ve 100, khong loi 422).
 * Java thuan, khong phu thuoc Spring Data.
 */
public final class PaginationSupport {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PaginationSupport() {
    }

    public static int normalizePage(Integer page) {
        return (page == null || page < 1) ? DEFAULT_PAGE : page;
    }

    public static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /** Dung meta = { "pagination": {page, size, total_items, total_pages} }. */
    public static Map<String, Object> meta(int page, int size, long totalItems) {
        long totalPages = totalItems == 0 ? 0 : (totalItems + size - 1) / size;
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        pagination.put("total_items", totalItems);
        pagination.put("total_pages", totalPages);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("pagination", pagination);
        return meta;
    }
}
