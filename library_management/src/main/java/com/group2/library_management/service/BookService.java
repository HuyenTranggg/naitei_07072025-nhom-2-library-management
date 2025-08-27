package com.group2.library_management.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.group2.library_management.dto.request.BookQueryParameters;
import com.group2.library_management.dto.request.CreateBookRequest;
import com.group2.library_management.dto.response.BookDetailResponse;
import com.group2.library_management.dto.request.UpdateBookRequest;
import com.group2.library_management.dto.response.BookResponse;
import com.group2.library_management.entity.Book;
import com.group2.library_management.exception.ResourceNotFoundException;

public interface BookService {
    Page<BookResponse> getAllBooks(BookQueryParameters params);

    /**
     * Lấy thông tin chi tiết của một đầu sách dựa vào ID.
     * 
     * @param bookId ID của sách cần tìm.
     * @return DTO chứa thông tin chi tiết của sách.
     * @throws ResourceNotFoundException nếu không tìm thấy sách.
     */
    BookDetailResponse getBookById(Integer bookId);

    void updateBook(Integer id, UpdateBookRequest request);

    UpdateBookRequest findBookForUpdate(Integer id);

    List<Book> findAll();

    void createBook(CreateBookRequest request);
}
