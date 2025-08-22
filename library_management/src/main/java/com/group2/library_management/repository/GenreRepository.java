package com.group2.library_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.group2.library_management.entity.Genre;

public interface GenreRepository extends JpaRepository<Genre, Integer> {
    
}
