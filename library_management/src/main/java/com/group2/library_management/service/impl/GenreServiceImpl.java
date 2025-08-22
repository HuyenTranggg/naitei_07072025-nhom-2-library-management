package com.group2.library_management.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group2.library_management.entity.Genre;
import com.group2.library_management.repository.GenreRepository;
import com.group2.library_management.service.GenreService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;

    @Override
    public List<Genre> findAll() {
        return genreRepository.findAll();
    }
    
}
