package com.group2.library_management.controller.admin;

import com.group2.library_management.dto.response.BookResponse;
import com.group2.library_management.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public String showBookList(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model
    ) {
        // Spring Pageable bắt đầu từ 0, nên cần trừ 1 từ page của người dùng
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("title").ascending());

        // Gọi service để lấy dữ liệu
        Page<BookResponse> bookPage = bookService.getAllBooks(keyword, pageable);

        // Đưa dữ liệu vào Model để Thymeleaf có thể render
        model.addAttribute("bookPage", bookPage);
        model.addAttribute("keyword", keyword);

        // Trả về tên của file view
        return "admin/book/list";
    }
}
