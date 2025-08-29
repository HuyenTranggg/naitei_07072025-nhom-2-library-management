package com.group2.library_management.repository;

import com.group2.library_management.entity.AuthorBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorBookRepository extends JpaRepository<AuthorBook, Integer> {
    // Xóa tất cả các bản ghi liên quan đến một bookId
    @Modifying
    @Query("DELETE FROM AuthorBook ab WHERE ab.book.id = :bookId")
    void deleteByBookId(Integer bookId);
}
