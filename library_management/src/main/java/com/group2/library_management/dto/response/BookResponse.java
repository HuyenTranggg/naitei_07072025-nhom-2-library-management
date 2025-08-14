package com.group2.library_management.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import com.group2.library_management.entity.Author;
import com.group2.library_management.entity.AuthorBook;
import com.group2.library_management.entity.Book;
import com.group2.library_management.entity.BookGenre;
import com.group2.library_management.entity.Edition;
import com.group2.library_management.entity.Genre;

// DTO đại diện cho thông tin 1 đầu sách hiển thị trên trang quản lý của admin

public record BookResponse(
    Integer id, 
    String title, 
    List<String> authors, 
    List<String> genres,
    List<Edition> editions
) {
    // phương thức factory tĩnh để chuyển đổi từ Book Entity sang BookResponse DTO
    public static BookResponse fromEntity(Book book) {
        // Lấy danh sách tên tác giả thông qua bảng trung gian AuthorBook
        List<String> authorNames = book.getAuthorBooks().stream()
                .map(AuthorBook::getAuthor) // Từ mỗi AuthorBook, lấy ra đối tượng Author
                .map(Author::getName)       // Từ mỗi Author, lấy ra tên
                .collect(Collectors.toList());

        // Lấy danh sách tên thể loại thông qua bảng trung gian BookGenre
        List<String> genreNames = book.getBookGenres().stream()
                .map(BookGenre::getGenre)   // Từ mỗi BookGenre, lấy ra đối tượng Genre
                .map(Genre::getName)        // Từ mỗi Genre, lấy ra tên
                .collect(Collectors.toList());

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                authorNames,
                genreNames,
                book.getEditions()
        );
    }
}
