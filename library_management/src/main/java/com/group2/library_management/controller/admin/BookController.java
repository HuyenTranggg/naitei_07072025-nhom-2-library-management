package com.group2.library_management.controller.admin;

import com.group2.library_management.dto.request.BookQueryParameters;
import com.group2.library_management.dto.mapper.BookMapper;
import com.group2.library_management.dto.request.UpdateBookRequest;
import com.group2.library_management.dto.response.BookResponse;
import com.group2.library_management.entity.Author;
import com.group2.library_management.entity.Book;
import com.group2.library_management.entity.Genre;
import com.group2.library_management.exception.ResourceNotFoundException;
import com.group2.library_management.service.AuthorService;
import com.group2.library_management.service.BookService;
import com.group2.library_management.service.GenreService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller("adminBookController")
@RequestMapping("/admin/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookMapper bookMapper;

    private final AuthorService authorService;
    private final GenreService genreService;

    private static final String VIEW_BOOK_EDIT = "admin/book/edit";
    private static final String REDIRECT_TO_BOOKS_LIST = "redirect:/admin/books";
    private static final String REDIRECT_TO_BOOK_EDIT = "redirect:/admin/books/%d/edit";

    private final MessageSource messageSource;

    @GetMapping
    public String showBookList(
            @ModelAttribute BookQueryParameters params,
            Model model
    ) {
        int oneBasedPage = Optional.ofNullable(params.page()).orElse(1);
        int zeroBasedPage = Math.max(0, oneBasedPage - 1);

        BookQueryParameters correctedParams = new BookQueryParameters(
                params.keyword(),
                params.genreIds(),
                params.genreMatchMode(),
                params.authorIds(),
                params.authorMatchMode(),
                zeroBasedPage,      
                params.size(),
                params.sort()
        );

        Page<BookResponse> bookPage = bookService.getAllBooks(correctedParams);

        // Add the retrieved data and search parameters to the model for the view
        model.addAttribute("bookPage", bookPage);
        model.addAttribute("keyword", correctedParams.keyword());

        // Return the logical view name for Thymeleaf to resolve
        return "admin/book/list";
    }

    @GetMapping("{id}/edit")
    public String showBookEditForm(@PathVariable("id") Integer id, Model model) {
        // Fetch the book details using the service layer
        Book book = bookService.getById(id);
        UpdateBookRequest requestDto = bookMapper.toUpdateRequest(book);

        List<Author> allAuthors = authorService.findAll();
        List<Genre> allGenres = genreService.findAll();

        model.addAttribute("bookRequest", requestDto);
        model.addAttribute("allAuthors", allAuthors);
        model.addAttribute("allGenres", allGenres);
        model.addAttribute("bookId", id);
        model.addAttribute("activeMenu", "books");
        return VIEW_BOOK_EDIT;
    }

    @PostMapping("/{id}")
    public String processBookUpdate(@PathVariable Integer id,
                                    @Valid @ModelAttribute("bookRequest") UpdateBookRequest request,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        
        if (bindingResult.hasErrors()) {
            // If errors, must send back the list of authors and genres
            model.addAttribute("allAuthors", authorService.findAll());
            model.addAttribute("allGenres", genreService.findAll());
            model.addAttribute("bookId", id);
            model.addAttribute("activeMenu", "books");
            return VIEW_BOOK_EDIT;
        }

        try {
            bookService.updateBook(id, request);
            String successMessage = messageSource.getMessage("admin.books.message.update_success", null, LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return REDIRECT_TO_BOOKS_LIST;
        } catch (ResourceNotFoundException e) {
            String errorMessage = messageSource.getMessage("common.error.not_found", null, LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return REDIRECT_TO_BOOKS_LIST;

        } catch (IllegalArgumentException e) {
            String errorMessage = messageSource.getMessage("admin.books.message.update_fail_specific", new Object[]{e.getMessage()}, LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return String.format(REDIRECT_TO_BOOK_EDIT, id);

        } catch (Exception e) {
            String errorMessage = messageSource.getMessage("common.error.unexpected", null, LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return String.format(REDIRECT_TO_BOOK_EDIT, id);
        }
    }

}
