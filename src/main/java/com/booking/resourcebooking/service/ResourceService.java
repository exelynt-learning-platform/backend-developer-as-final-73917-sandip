package com.booking.resourcebooking.service;

import com.booking.resourcebooking.dto.resource.ResourceRequest;
import com.booking.resourcebooking.dto.resource.ResourceResponse;
import com.booking.resourcebooking.entity.Resource;
import com.booking.resourcebooking.exception.ResourceNotFoundException;
import com.booking.resourcebooking.mapper.ResourceMapper;
import com.booking.resourcebooking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public Page<ResourceResponse> getAll(Pageable pageable) {
        return resourceRepository.findAll(pageable).map(ResourceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getById(Long id) {
        return ResourceMapper.toResponse(findEntityOrThrow(id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .available(request.getAvailable() == null || request.getAvailable())
                .build();
        return ResourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = findEntityOrThrow(id);
        resource.setName(request.getName());
        resource.setType(request.getType());
        resource.setDescription(request.getDescription());
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }
        return ResourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        Resource resource = findEntityOrThrow(id);
        resourceRepository.delete(resource);
    }

    Resource findEntityOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }
}
