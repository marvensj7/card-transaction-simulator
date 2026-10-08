package com.marvens.capstone.dto;

import java.util.List;
import org.springframework.data.domain.Page;

// A small stable JSON contract shared by the three paginated lists.
public class PageResponse<T> {
    public final List<T> items;
    public final int page;
    public final int size;
    public final long totalElements;
    public final int totalPages;

    public PageResponse(List<T> items, Page<?> source) {
        this.items = items;
        page = source.getNumber();
        size = source.getSize();
        totalElements = source.getTotalElements();
        totalPages = source.getTotalPages();
    }
}
