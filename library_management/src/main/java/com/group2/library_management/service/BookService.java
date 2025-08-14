package com.group2.library_management.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.group2.library_management.dto.response.BookResponse;

public interface BookService {
    // lấy danh sách các đầu sách đã được phân trang và lọc
    Page<BookResponse> getAllBooks(String keyword, Pageable pageable);
}
