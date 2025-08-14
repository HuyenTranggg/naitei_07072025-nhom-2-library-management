package com.group2.library_management.service.impl;

import com.group2.library_management.dto.response.BookResponse;
import com.group2.library_management.entity.Author;
import com.group2.library_management.entity.AuthorBook; 
import com.group2.library_management.entity.Book;
import com.group2.library_management.repository.BookRepository;
import com.group2.library_management.service.BookService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Override
    public Page<BookResponse> getAllBooks(String keyword, Pageable pageable) {
        Specification<Book> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.toLowerCase().trim() + "%";

                // Join Book -> AuthorBook -> Author to find by author name
                Join<Book, AuthorBook> authorBookJoin = root.join("authorBooks", JoinType.LEFT);
                Join<AuthorBook, Author> authorJoin = authorBookJoin.join("author", JoinType.LEFT);

                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate authorNameLike = cb.like(cb.lower(authorJoin.get("name")), pattern);

                predicates.add(cb.or(titleLike, authorNameLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Book> bookPage = bookRepository.findAll(spec, pageable);
        return bookPage.map(BookResponse::fromEntity);
    }
}
