package com.educmind.notification.dto;

import java.util.List;

public record NotificationPageResult(List<NotificationResponse> items, int limit,
                                     long latestSeq, boolean hasMore) {
}
