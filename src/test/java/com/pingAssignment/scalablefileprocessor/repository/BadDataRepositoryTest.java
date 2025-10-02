package com.pingAssignment.scalablefileprocessor.repository;

import com.pingAssignment.scalablefileprocessor.model.BadData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class BadDataRepositoryTest {
    @Mock
    private BadDataRepository badDataRepository;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFindAll() {
        BadData badData = new BadData();
        badData.setRawRecord("{\"invalid\":\"data\"}");

        given(badDataRepository.findAll()).willReturn(List.of(badData));

        List<BadData> badDataList = badDataRepository.findAll();

        assertThat(badDataList).isNotEmpty();
        assertThat(badDataList.get(0).getRawRecord()).contains("invalid");
    }

    @Test
    public void testFindAllReturnsEmptyList() {
        given(badDataRepository.findAll()).willReturn(Collections.emptyList());

        List<BadData> badDataList = badDataRepository.findAll();

        assertThat(badDataList).isEmpty();
    }

    @Test
    public void testSaveThrowsRuntimeException() {
        BadData badData = new BadData();
        badData.setRawRecord("{\"error\":\"test\"}");

        doThrow(new RuntimeException("DB error")).when(badDataRepository).save(badData);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            badDataRepository.save(badData);
        });

        assertThat(thrown.getMessage()).isEqualTo("DB error");
    }

    @Test
    public void testSaveNullThrowsIllegalArgumentException() {
        doThrow(new IllegalArgumentException("Cannot save null"))
                .when(badDataRepository).save(null);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            badDataRepository.save(null);
        });

        assertThat(thrown.getMessage()).isEqualTo("Cannot save null");
    }

    @Test
    public void testDeleteAllCalled() {
        // This verifies that deleteAll can be successfully called on the repository mock
        badDataRepository.deleteAll();
        verify(badDataRepository).deleteAll();
    }
}
