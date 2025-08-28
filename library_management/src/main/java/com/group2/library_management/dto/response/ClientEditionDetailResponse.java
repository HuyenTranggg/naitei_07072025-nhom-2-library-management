package com.group2.library_management.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.group2.library_management.entity.enums.BookFormat;

public record ClientEditionDetailResponse(
        Integer id,
        String title,
        String bookTitle,
        List<String> authors,
        List<String> genres,
        String isbn,
        String publisherName,
        LocalDate publicationDate,
        String language,
        Integer pageNumber,
        String coverImageUrl,
        BookFormat format,
        String description,
        int availableQuantity
) {}
