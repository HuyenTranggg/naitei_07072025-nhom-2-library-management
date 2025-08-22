package com.group2.library_management.service.impl;

import com.group2.library_management.common.constants.PaginationConstants;
import com.group2.library_management.dto.enums.MatchMode;
import com.group2.library_management.dto.mapper.BookMapper;
import com.group2.library_management.dto.request.BookQueryParameters;
import com.group2.library_management.dto.response.BookDetailResponse;
import com.group2.library_management.dto.request.UpdateBookRequest;
import com.group2.library_management.dto.response.BookResponse;
import com.group2.library_management.entity.Author;
import com.group2.library_management.entity.AuthorBook;
import com.group2.library_management.entity.Book;
import com.group2.library_management.exception.ResourceNotFoundException;
import com.group2.library_management.entity.BookGenre;
import com.group2.library_management.entity.Genre;
import com.group2.library_management.exception.ResourceNotFoundException;
import com.group2.library_management.repository.AuthorRepository;
import com.group2.library_management.repository.BookRepository;
import com.group2.library_management.repository.specification.BookSpecification;
import com.group2.library_management.service.BookService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.group2.library_management.repository.GenreRepository;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "title"); 

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;

    private final MessageSource messageSource;

    @Override
    public Page<BookResponse> getAllBooks(BookQueryParameters params) {

        int page = Optional.ofNullable(params.page()).orElse(PaginationConstants.DEFAULT_PAGE_NUMBER);
        int size = Optional.ofNullable(params.size()).orElse(PaginationConstants.DEFAULT_PAGE_SIZE);
        String[] sortParams = Optional.ofNullable(params.sort()).orElse(PaginationConstants.DEFAULT_SORT);
        
        // Xử lý tham số sắp xếp
        String sortField = sortParams[0];
        if (!StringUtils.hasText(sortField) || !ALLOWED_SORT_FIELDS.contains(sortField)) { 
            sortField = PaginationConstants.DEFAULT_SORT[0]; 
        }
        Sort.Direction direction = sortParams[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;

        Sort.Order order = new Sort.Order(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, Sort.by(order));

        Specification<Book> spec = Specification.unrestricted();

        if (StringUtils.hasText(params.keyword())) {
            spec = spec.and(BookSpecification.searchByKeyword(params.keyword()));
        }

        if (!CollectionUtils.isEmpty(params.genreIds())) {
            MatchMode mode = Optional.ofNullable(params.genreMatchMode()).orElse(MatchMode.ANY); 

            if (mode == MatchMode.ALL) {
                spec = spec.and(BookSpecification.hasAllGenres(params.genreIds()));
            } else {
                spec = spec.and(BookSpecification.hasAnyGenre(params.genreIds()));
            }
        }

        if (!CollectionUtils.isEmpty(params.authorIds())) {
            MatchMode mode = Optional.ofNullable(params.authorMatchMode()).orElse(MatchMode.ANY);
            
            if (mode == MatchMode.ALL) {
                spec = spec.and(BookSpecification.hasAllAuthors(params.authorIds()));
            } else {
                spec = spec.and(BookSpecification.hasAnyAuthor(params.authorIds()));
            }
        }

        Page<Book> bookPage = bookRepository.findAll(spec, pageable);

        return bookPage.map(bookMapper::toBookResponse);
    }

    @Override
    public BookDetailResponse getBookById(Integer bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException());

        return bookMapper.toBookDetailResponse(book);
    }

    @Override
    public Book getById(Integer id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> {
                    String message = messageSource.getMessage(
                        "common.error.resource_not_found",
                        new Object[]{"Book", id},
                        LocaleContextHolder.getLocale()
                    );
                    return new ResourceNotFoundException(message);
                });
    }

    @Override
    @Transactional 
    public void updateBook(Integer id, UpdateBookRequest request) {
        // get book
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> {
                    String message = messageSource.getMessage(
                        "common.error.resource_not_found",
                        new Object[]{"Book", id},
                        LocaleContextHolder.getLocale()
                    );
                    return new ResourceNotFoundException(message);
                });

        // mapper
        bookMapper.updateFromRequest(request, book);

        // get entity Author and Genre
        List<Author> authors = authorRepository.findAllById(request.getAuthorIds());
        List<Genre> genres = genreRepository.findAllById(request.getGenreIds());

        book.getAuthorBooks().clear();
        book.getBookGenres().clear();
    
        authors.forEach(author -> {
            AuthorBook authorBook = AuthorBook.builder()
                    .book(book)
                    .author(author)
                    .build();
            book.getAuthorBooks().add(authorBook);
        });
        
        genres.forEach(genre -> {
            BookGenre bookGenre = BookGenre.builder()
                    .book(book)
                    .genre(genre)
                    .build();
            book.getBookGenres().add(bookGenre);
        });

        // save
        bookRepository.save(book);
    }
}
