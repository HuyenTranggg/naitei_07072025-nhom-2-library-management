package com.group2.library_management.dto.response;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.group2.library_management.entity.Author;
import com.group2.library_management.entity.AuthorBook;
import com.group2.library_management.entity.Book;
import com.group2.library_management.entity.BookGenre;
import com.group2.library_management.entity.Edition;
import com.group2.library_management.entity.Genre;

// This DTO is optimized to only carry data necessary for the view layer

public record BookResponse(
    Integer id, 
    String title, 
    List<String> authors, 
    List<String> genres,
    int editionCount
) {
    // A static factory method to convert a Book entity into an optimized BookResponse DTO
    public static BookResponse fromEntity(Book book) {
        // Get author
        List<String> authorNames = Optional.ofNullable(book.getAuthorBooks())
                .orElse(Collections.emptyList())
                .stream()
                .map(AuthorBook::getAuthor)
                .map(Author::getName)
                .collect(Collectors.toList());

        // Get genres
        List<String> genreNames = Optional.ofNullable(book.getBookGenres())
                .orElse(Collections.emptyList())
                .stream()
                .map(BookGenre::getGenre)
                .map(Genre::getName)
                .collect(Collectors.toList());

        // Get editions count
        List<Edition> editions = Optional.ofNullable(book.getEditions()).orElse(Collections.emptyList());
        int count = editions.size();

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                authorNames,
                genreNames,
                count
        );
    }
}
