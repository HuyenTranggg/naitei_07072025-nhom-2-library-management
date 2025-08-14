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

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    public String showBookList(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model
    ) {
        // --- Input Validation ---
        // Ensure the page number is not less than 1.
        int validatedPage = Math.max(1, page);

        // Ensure the page size is within a reasonable range (e.g., 1 to 100).
        int validatedSize = (size > 0 && size <= MAX_PAGE_SIZE) ? size : DEFAULT_PAGE_SIZE;

        Pageable pageable = PageRequest.of(validatedPage - 1, validatedSize, Sort.by("title").ascending());

        // Call the service layer to fetch the paginated list of books
        Page<BookResponse> bookPage = bookService.getAllBooks(keyword, pageable);

        // Add the retrieved data and search parameters to the model for the view
        model.addAttribute("bookPage", bookPage);
        model.addAttribute("keyword", keyword);

        // Return the logical view name for Thymeleaf to resolve
        return "admin/book/list";
    }
}
