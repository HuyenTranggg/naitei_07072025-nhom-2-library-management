package com.group2.library_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.group2.library_management.entity.Author;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Integer>{
    
}
