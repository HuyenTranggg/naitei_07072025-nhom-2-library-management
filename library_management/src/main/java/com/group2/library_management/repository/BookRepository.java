package com.group2.library_management.repository;

import com.group2.library_management.entity.Book;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer>, JpaSpecificationExecutor<Book> { 
    List<Book> findByTitleIgnoreCase(String title);

    /**
     * Hard delete a Book by ID.
     * @param id ID of the Book to delete.
     */
    @Modifying
    @Query("DELETE FROM Book b WHERE b.id = :id")
    void hardDeleteById(Integer id);

}
