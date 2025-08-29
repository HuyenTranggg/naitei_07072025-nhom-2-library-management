package com.group2.library_management.repository;

import com.group2.library_management.entity.BookGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BookGenreRepository extends JpaRepository<BookGenre, Integer> {
    @Modifying
    @Query("DELETE FROM BookGenre bg WHERE bg.book.id = :bookId")
    void deleteByBookId(Integer bookId);
}
