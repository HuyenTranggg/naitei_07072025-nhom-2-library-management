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
import com.group2.library_management.entity.Edition;
import com.group2.library_management.entity.Genre;
import com.group2.library_management.exception.ResourceNotFoundException;
import com.group2.library_management.repository.AuthorRepository;
import com.group2.library_management.repository.BookRepository;
import com.group2.library_management.repository.specification.BookSpecification;
import com.group2.library_management.service.BookService;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.group2.library_management.repository.GenreRepository;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;

    private final BookMapper bookMapper;

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
    public UpdateBookRequest findBookForUpdate(Integer id) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> {
                String message = messageSource.getMessage(
                    "common.error.not_found", new Object[]{id}, LocaleContextHolder.getLocale()
                );
                return new ResourceNotFoundException(message);
            });
        return bookMapper.toUpdateRequest(book);
    }

    @Override
    @Transactional 
    public void updateBook(Integer id, UpdateBookRequest request) {
        // get book
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> {
                    String message = messageSource.getMessage(
                        "common.error.not_found",
                        new Object[]{id},
                        LocaleContextHolder.getLocale()
                    );
                    return new ResourceNotFoundException(message);
                });

        // mapper
        bookMapper.updateFromRequest(request, book);

        // get entity Author and Genre
        List<Author> authors = authorRepository.findAllById(request.getAuthorIds());
        // Kiểm tra xem tất cả các tác giả yêu cầu có tồn tại không
        if (authors.size() != request.getAuthorIds().size()) {
            List<Integer> foundAuthorIds = authors.stream().map(Author::getId).toList();
            List<Integer> notFoundAuthorIds = request.getAuthorIds().stream()
                    .filter(authorId -> !foundAuthorIds.contains(authorId))
                    .toList();
            String message = messageSource.getMessage(
                "book.error.authors_not_found", 
                new Object[]{notFoundAuthorIds},
                LocaleContextHolder.getLocale()
            );
            throw new ResourceNotFoundException(message);
        }

        List<Genre> genres = genreRepository.findAllById(request.getGenreIds());
        // Kiểm tra xem tất cả các thể loại yêu cầu có tồn tại không
        if (genres.size() != request.getGenreIds().size()) {
            List<Integer> foundGenreIds = genres.stream().map(Genre::getId).toList();
            List<Integer> notFoundGenreIds = request.getGenreIds().stream()
                    .filter(genreId -> !foundGenreIds.contains(genreId))
                    .toList();
            String message = messageSource.getMessage(
                "book.error.genres_not_found",
                new Object[]{notFoundGenreIds},
                LocaleContextHolder.getLocale()
            );
            throw new ResourceNotFoundException(message);
        }

        // Xử lý cập nhật AuthorBooks
        // Nếu danh sách tác giả thay đổi, mới cần xóa và thêm lại
        Set<Integer> currentAuthorIds = book.getAuthorBooks().stream()
                .map(ab -> ab.getAuthor().getId())
                .collect(Collectors.toSet());
        Set<Integer> newAuthorIds = new HashSet<>(request.getAuthorIds());

        if (!currentAuthorIds.equals(newAuthorIds)) {
            book.getAuthorBooks().clear();
            authors.forEach(author -> {
                AuthorBook authorBook = AuthorBook.builder()
                        .book(book)
                        .author(author)
                        .build();
                book.getAuthorBooks().add(authorBook);
            });
        }

        // Xử lý cập nhật BookGenres
        // Nếu danh sách thể loại thay đổi, mới cần xóa và thêm lại
        Set<Integer> currentGenreIds = book.getBookGenres().stream()
                .map(bg -> bg.getGenre().getId())
                .collect(Collectors.toSet());
        Set<Integer> newGenreIds = new HashSet<>(request.getGenreIds());

        if (!currentGenreIds.equals(newGenreIds)) {
            book.getBookGenres().clear();
            genres.forEach(genre -> {
                BookGenre bookGenre = BookGenre.builder()
                        .book(book)
                        .genre(genre)
                        .build();
                book.getBookGenres().add(bookGenre);
            });
        }

        // save
        try {
            bookRepository.save(book);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Xử lý lỗi khi có xung đột phiên bản
            String message = messageSource.getMessage("error.book.optimistic_lock", null, LocaleContextHolder.getLocale());
            throw new OptimisticLockingFailureException(message);
        } catch (DataIntegrityViolationException e) {
            // Xử lý các lỗi vi phạm ràng buộc CSDL khác
            String message = messageSource.getMessage("error.book.data_integrity_violation", null, LocaleContextHolder.getLocale());
            throw new DataIntegrityViolationException(message, e);
        }
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }
}
