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
import com.group2.library_management.dto.response.ClientEditionDetailResponse;
import com.group2.library_management.dto.request.CreateEditionRequest;
import com.group2.library_management.dto.response.EditionDetailResponse;
import com.group2.library_management.dto.response.EditionListResponse;
import com.group2.library_management.dto.response.EditionResponse;
import com.group2.library_management.dto.response.EditionUpdateResponse;
import com.group2.library_management.entity.Book;
import com.group2.library_management.entity.BookInstance;
import com.group2.library_management.entity.Edition;
import com.group2.library_management.entity.Publisher;
import com.group2.library_management.entity.enums.DeletionStatus;
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

    /**
     * @param edition Entity source
     * @param deletionStatus delete status
     * @return DTO to show on sub-table.
     */
    @Mapping(source = "edition.publisher.name", target = "publisherName")
    @Mapping(source = "edition.publicationDate.year", target = "publicationYear")
    public abstract EditionResponse toResponseDto(Edition edition, DeletionStatus deletionStatus);

    @Mapping(source = "publisher.id", target = "publisherId")
    public abstract EditionUpdateResponse toUpdateResponse(Edition edition);

    @Mapping(target = "publisher", ignore = true)
    public abstract void updateFromRequest(UpdateEditionRequest request, @MappingTarget Edition edition);
    
    public ClientEditionDetailResponse toClientDetailDto(Edition edition) {
        if (edition == null) {
            return null;
        }

        Book book = edition.getBook();
        Publisher publisher = edition.getPublisher();

        List<String> authorNames = Optional.ofNullable(book)
                .map(Book::getAuthorBooks)
                .orElse(Collections.emptyList())
                .stream()
                .map(authorBook -> authorBook.getAuthor().getName())
                .collect(Collectors.toList());

        List<String> genreNames = Optional.ofNullable(book)
                .map(Book::getBookGenres)
                .orElse(Collections.emptyList())
                .stream()
                .map(bookGenre -> bookGenre.getGenre().getName())
                .collect(Collectors.toList());

        return new ClientEditionDetailResponse(
                edition.getId(),
                edition.getTitle(),
                book != null ? book.getTitle() : null,
                authorNames,
                genreNames,
                edition.getIsbn(),
                publisher != null ? publisher.getName() : null,
                edition.getPublicationDate(),
                edition.getLanguage(),
                edition.getPageNumber(),
                edition.getCoverImageUrl(),
                edition.getFormat(),
                book != null ? book.getDescription() : null,
                edition.getAvailableQuantity() 
        );
    }
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "publisher", ignore = true)
    @Mapping(target = "coverImageUrl", ignore = true)
    @Mapping(target = "bookInstances", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    public abstract Edition toEntity(CreateEditionRequest request);
} 
