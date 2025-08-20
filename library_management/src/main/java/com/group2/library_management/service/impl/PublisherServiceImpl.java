package com.group2.library_management.service.impl;

import com.group2.library_management.entity.Publisher;
import com.group2.library_management.repository.PublisherRepository;
import com.group2.library_management.service.PublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {

    private final PublisherRepository publisherRepository;

    @Override
    public List<Publisher> findAll() {
        return publisherRepository.findAll();
    }
}
