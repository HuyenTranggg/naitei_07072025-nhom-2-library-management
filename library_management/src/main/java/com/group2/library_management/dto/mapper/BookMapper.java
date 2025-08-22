package com.group2.library_management.dto.mapper;

import com.group2.library_management.dto.response.BookDetailResponse;
import com.group2.library_management.dto.response.BookResponse;
import com.group2.library_management.dto.response.EditionSummaryResponse;
import com.group2.library_management.dto.request.UpdateBookRequest;
import com.group2.library_management.entity.Author;
import com.group2.library_management.entity.AuthorBook;
import com.group2.library_management.entity.Book;
import com.group2.library_management.entity.BookGenre;
import com.group2.library_management.entity.BookInstance;
import com.group2.library_management.entity.Edition;
import com.group2.library_management.entity.Genre;
import com.group2.library_management.entity.enums.BookStatus;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;
import org.mapstruct.MappingTarget;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookMapper {

    /**
     * Chuyển đổi từ Book entity sang BookResponse DTO.
     * 
     * @param book Entity Book
     * @return DTO BookResponse
     */
    @Mapping(source = "authorBooks", target = "authors", qualifiedByName = "authorBooksToAuthorNames")
    @Mapping(source = "bookGenres", target = "genres", qualifiedByName = "bookGenresToGenreNames")
    @Mapping(target = "editionCount", expression = "java(book.getEditions() != null ? book.getEditions().size() : 0)")
    BookResponse toBookResponse(Book book);

    /**
     * Chuyển đổi List<AuthorBook> sang List<String>.
     */
    @Named("authorBooksToAuthorNames")
    default List<String> authorBooksToAuthorNames(List<AuthorBook> authorBooks) {
        if (authorBooks == null || authorBooks.isEmpty()) {
            return Collections.emptyList(); 
        }
        return authorBooks.stream()
                .map(AuthorBook::getAuthor)
                .map(Author::getName)
                .toList();
    }

    /**
     * Chuyển đổi List<BookGenre> sang List<String>.
     */
    @Named("bookGenresToGenreNames")
    default List<String> bookGenresToGenreNames(List<BookGenre> bookGenres) {
        if (bookGenres == null || bookGenres.isEmpty()) {
            return Collections.emptyList();
        }
        return bookGenres.stream()
                .map(BookGenre::getGenre)
                .map(Genre::getName)
                .toList();
    }

    /**
     * Chuyển đổi từ Book entity sang BookDetailResponse DTO.
     * 
     * @param book Entity Book
     * @return DTO BookDetailResponse
     */
    @Mapping(source = "authorBooks", target = "authors", qualifiedByName = "authorBooksToAuthorNames")
    @Mapping(source = "bookGenres", target = "genres", qualifiedByName = "bookGenresToGenreNames")
    BookDetailResponse toBookDetailResponse(Book book);

    @Mapping(source = "publisher.name", target = "publisher")
    @Mapping(source = "publicationDate", target = "publicationDate", dateFormat = "dd-MM-yyyy")
    @Mapping(source = "bookInstances", target = "bookinstanceCount", qualifiedByName = "countAvailableInstances")
    EditionSummaryResponse toEditionSummaryResponse(Edition edition);

    /**
     * Phương thức dùng để đếm các BookInstance có status là AVAILABLE.
     * 
     * @param bookInstances danh sách tất cả các bản sách vật lý của một edition.
     * @return số lượng bản sách vật lý có sẵn.
     */
    @Named("countAvailableInstances")
    default Integer countAvailableInstances(List<BookInstance> bookInstances) {
        if (bookInstances == null || bookInstances.isEmpty()) {
            return 0;
        }

        return (int) bookInstances.stream()
                .filter(instance -> instance.getStatus() == BookStatus.AVAILABLE)
                .count();
    }

    @Mapping(source = "authorBooks", target = "authorIds", qualifiedByName = "authorBooksToIds")
    @Mapping(source = "bookGenres", target = "genreIds", qualifiedByName = "bookGenresToIds")
    UpdateBookRequest toUpdateRequest(Book book);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "editions", ignore = true)
    @Mapping(target = "authorBooks", ignore = true)
    @Mapping(target = "bookGenres", ignore = true)
    void updateFromRequest(UpdateBookRequest request, @MappingTarget Book book);

    @Named("authorBooksToIds")
    default List<Integer> authorBooksToIds(List<AuthorBook> authorBooks) {
        if (authorBooks == null || authorBooks.isEmpty()) {
            return Collections.emptyList();
        }
        return authorBooks.stream()
                .map(AuthorBook::getAuthor)
                .map(Author::getId)
                .collect(Collectors.toList());
    }


    @Named("bookGenresToIds")
    default List<Integer> bookGenresToIds(List<BookGenre> bookGenres) {
        if (bookGenres == null || bookGenres.isEmpty()) {
            return Collections.emptyList();
        }
        return bookGenres.stream()
                .map(BookGenre::getGenre)
                .map(Genre::getId)
                .collect(Collectors.toList());
    }
}
