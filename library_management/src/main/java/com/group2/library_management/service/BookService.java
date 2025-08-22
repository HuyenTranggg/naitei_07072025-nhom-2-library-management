package com.group2.library_management.service;

import org.springframework.data.domain.Page;

import com.group2.library_management.dto.request.BookQueryParameters;
import com.group2.library_management.dto.response.BookDetailResponse;
import com.group2.library_management.dto.request.UpdateBookRequest;
import com.group2.library_management.dto.response.BookResponse;
import com.group2.library_management.entity.Book;

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
    Book getById(Integer id);
    void updateBook(Integer id, UpdateBookRequest request);
}
