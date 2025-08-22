package com.group2.library_management.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.group2.library_management.entity.Author;
import com.group2.library_management.repository.AuthorRepository;
import com.group2.library_management.service.AuthorService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    // Implement the methods defined in AuthorService interface
    @Override
    public List<Author> findAll() {
        return authorRepository.findAll();
    }
    
}
