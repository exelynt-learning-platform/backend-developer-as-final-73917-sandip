package com.booking.resourcebooking.mapper;

import com.booking.resourcebooking.dto.resource.ResourceResponse;
import com.booking.resourcebooking.entity.Resource;

public final class ResourceMapper {

    private ResourceMapper() {
    }

    public static ResourceResponse toResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .type(resource.getType())
                .description(resource.getDescription())
                .available(resource.isAvailable())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
