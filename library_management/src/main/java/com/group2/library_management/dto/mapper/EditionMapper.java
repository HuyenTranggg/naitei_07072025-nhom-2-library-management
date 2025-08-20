package com.group2.library_management.dto.mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Collections;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import com.group2.library_management.dto.request.UpdateEditionRequest;
import com.group2.library_management.dto.response.EditionDetailResponse;
import com.group2.library_management.dto.response.EditionListResponse;
import com.group2.library_management.entity.Book;
import com.group2.library_management.entity.BookInstance;
import com.group2.library_management.entity.Edition;
import com.group2.library_management.entity.Publisher;
import com.group2.library_management.service.EnumDisplayService;

import lombok.RequiredArgsConstructor;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@RequiredArgsConstructor
public abstract class EditionMapper {
    public EditionListResponse toDto(Edition edition) {
        if (edition == null) {
            return null;
        }
        EditionListResponse.EditionListResponseBuilder editionResponse =  EditionListResponse.builder()
                .id(edition.getId())
                .title(edition.getTitle())
                .isbn(edition.getIsbn() != null ? edition.getIsbn() : null)
                .publisher(edition.getPublisher() != null ? edition.getPublisher().getName() : null)
                .publicationYear(edition.getPublicationDate() != null ? edition.getPublicationDate().getYear() : null)
                .language(edition.getLanguage() != null ? edition.getLanguage() : null);
        List<BookInstance> bookInstances = Optional.ofNullable(edition.getBookInstances()).orElse(Collections.emptyList());
        int count = bookInstances.size();
        return editionResponse.bookinstanceCount(count).build();   
    }
    
    @Autowired
    private EnumDisplayService enumDisplayService;

    public EditionDetailResponse toDetailDto(Edition edition) {
        if (edition == null) {
            return null;
        }

        Book book = edition.getBook();
        Publisher publisher = edition.getPublisher();

        // if book is null, authorNames will be an empty list
        List<String> authorNames = Optional.ofNullable(book)
                .map(Book::getAuthorBooks)
                .orElse(Collections.emptyList())
                .stream()
                .map(authorBook -> authorBook.getAuthor().getName())
                .collect(Collectors.toList());

        // Using injected service to get display name of format
        String formatString = enumDisplayService.getDisplayName(edition.getFormat());

        // Create new DTO
        return new EditionDetailResponse(
                edition.getId(),
                edition.getTitle(),
                book != null ? book.getTitle() : null,
                authorNames,
                edition.getIsbn(),
                publisher != null ? publisher.getName() : null,
                edition.getPublicationDate(),
                edition.getLanguage(),
                edition.getPageNumber(),
                edition.getCoverImageUrl(),
                formatString,
                book != null ? book.getDescription() : null,
                edition.getInitialQuantity(),
                edition.getAvailableQuantity()
        );
    }

    // Entity -> Request DTO
    @Mapping(source = "publisher.id", target = "publisherId")
    public abstract UpdateEditionRequest toUpdateRequest(Edition edition);

    // Request DTO -> Entity
    // Ignore publisherId because we will handle it in the service
    @Mapping(target = "publisher", ignore = true) 
    public abstract void updateFromRequest(UpdateEditionRequest request, @MappingTarget Edition edition);
} 
