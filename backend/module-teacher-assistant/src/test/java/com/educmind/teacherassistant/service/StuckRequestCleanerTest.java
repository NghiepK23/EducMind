package com.educmind.teacherassistant.service;

import com.educmind.teacherassistant.config.TeacherAssistantProperties;
import com.educmind.teacherassistant.repository.AiGenerationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StuckRequestCleanerTest {

    private AiGenerationRequestRepository repository;
    private TeacherAssistantProperties properties;
    private StuckRequestCleaner cleaner;

    @BeforeEach
    void setUp() {
        repository = mock(AiGenerationRequestRepository.class);
        properties = new TeacherAssistantProperties();
        cleaner = new StuckRequestCleaner(repository, properties);
    }

    @Test
    void macDinh10Phut_truyenDungMocThoiGianVaThongBao() {
        when(repository.failStuck(any(), any(), anyString())).thenReturn(2);

        OffsetDateTime before = OffsetDateTime.now();
        int n = cleaner.sweep();
        OffsetDateTime after = OffsetDateTime.now();

        assertThat(n).isEqualTo(2);
        ArgumentCaptor<OffsetDateTime> cutoff = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(repository).failStuck(cutoff.capture(), any(), message.capture());
        assertThat(cutoff.getValue()).isAfterOrEqualTo(before.minusMinutes(10));
        assertThat(cutoff.getValue()).isBeforeOrEqualTo(after.minusMinutes(10));
        assertThat(message.getValue()).isEqualTo(StuckRequestCleaner.MESSAGE);
    }

    @Test
    void doiNguongCauHinh_mocThoiGianDoiTheo() {
        properties.setStuckMinutes(3);
        when(repository.failStuck(any(), any(), anyString())).thenReturn(0);

        OffsetDateTime before = OffsetDateTime.now();
        int n = cleaner.sweep();
        OffsetDateTime after = OffsetDateTime.now();

        assertThat(n).isZero();
        ArgumentCaptor<OffsetDateTime> cutoff = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).failStuck(cutoff.capture(), any(), anyString());
        assertThat(cutoff.getValue()).isAfterOrEqualTo(before.minusMinutes(3));
        assertThat(cutoff.getValue()).isBeforeOrEqualTo(after.minusMinutes(3));
    }
}
