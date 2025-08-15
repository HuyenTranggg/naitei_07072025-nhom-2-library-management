package com.group2.library_management.dto.response;

import com.group2.library_management.entity.Edition;

public record EditionResponse(
    Integer id,
    String title,
    String isbn,
    String publisherName,
    Integer publicationYear,
    int availableQuantity
) {
    public static EditionResponse fromEntity(Edition edition) {
        // Lấy năm từ publication date
        Integer year = (edition.getPublicationDate() != null) ? edition.getPublicationDate().getYear() : null;

        return new EditionResponse(
            edition.getId(),
            edition.getTitle(),
            edition.getIsbn(),
            edition.getPublisher().getName(),
            year,
            edition.getAvailableQuantity()
        );
    }
}
