package com.template.usermanagement.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    @DisplayName("of(Page) maps content correctly")
    void of_shouldMapContent() {
        List<String> items = List.of("alpha", "beta", "gamma");
        Page<String> page = new PageImpl<>(items, PageRequest.of(0, 10), 3);

        PageResponse<String> response = PageResponse.of(page);

        assertEquals(items, response.getContent());
    }

    @Test
    @DisplayName("of(Page) maps page number correctly")
    void of_shouldMapPageNumber() {
        Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(2, 5), 20);

        PageResponse<String> response = PageResponse.of(page);

        assertEquals(2, response.getPage());
    }

    @Test
    @DisplayName("of(Page) maps size correctly")
    void of_shouldMapSize() {
        Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(0, 25), 1);

        PageResponse<String> response = PageResponse.of(page);

        assertEquals(25, response.getSize());
    }

    @Test
    @DisplayName("of(Page) maps totalElements correctly")
    void of_shouldMapTotalElements() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 10), 42);

        PageResponse<String> response = PageResponse.of(page);

        assertEquals(42, response.getTotalElements());
    }

    @Test
    @DisplayName("of(Page) maps totalPages correctly")
    void of_shouldMapTotalPages() {
        Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(0, 10), 35);

        PageResponse<String> response = PageResponse.of(page);

        // 35 elements / 10 per page = 4 pages (ceil)
        assertEquals(4, response.getTotalPages());
    }

    @Test
    @DisplayName("of(Page) maps first=true for first page")
    void of_firstPage_shouldMapFirstTrue() {
        Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(0, 10), 20);

        PageResponse<String> response = PageResponse.of(page);

        assertTrue(response.isFirst());
    }

    @Test
    @DisplayName("of(Page) maps first=false for non-first page")
    void of_nonFirstPage_shouldMapFirstFalse() {
        Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(1, 10), 20);

        PageResponse<String> response = PageResponse.of(page);

        assertFalse(response.isFirst());
    }

    @Test
    @DisplayName("of(Page) maps last=true for last page")
    void of_lastPage_shouldMapLastTrue() {
        Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(1, 10), 20);

        PageResponse<String> response = PageResponse.of(page);

        assertTrue(response.isLast());
    }

    @Test
    @DisplayName("of(Page) maps last=false for non-last page")
    void of_nonLastPage_shouldMapLastFalse() {
        Page<String> page = new PageImpl<>(List.of("a", "b", "c"), PageRequest.of(0, 3), 10);

        PageResponse<String> response = PageResponse.of(page);

        assertFalse(response.isLast());
    }

    @Test
    @DisplayName("of(Page) with empty page maps correctly")
    void of_emptyPage_shouldMapCorrectly() {
        Page<String> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        PageResponse<String> response = PageResponse.of(page);

        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    @DisplayName("of(Page) with single-element single-page maps all fields")
    void of_singleElementPage_shouldMapAllFields() {
        Page<Integer> page = new PageImpl<>(List.of(99), PageRequest.of(0, 5), 1);

        PageResponse<Integer> response = PageResponse.of(page);

        assertEquals(List.of(99), response.getContent());
        assertEquals(0, response.getPage());
        assertEquals(5, response.getSize());
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }
}
