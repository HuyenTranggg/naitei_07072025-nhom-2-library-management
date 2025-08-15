package com.group2.library_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group2.library_management.entity.Edition;

@Repository
public interface EditionRepository extends JpaRepository<Edition, Integer>{
    List<Edition> findByBookIdOrderByPublicationDateDesc(Integer bookId);
}
