package com.booking.resourcebooking.service;

import com.booking.resourcebooking.dto.reservation.ReservationRequest;
import com.booking.resourcebooking.dto.reservation.ReservationResponse;
import com.booking.resourcebooking.dto.reservation.ReservationStatusUpdateRequest;
import com.booking.resourcebooking.entity.Reservation;
import com.booking.resourcebooking.entity.ReservationStatus;
import com.booking.resourcebooking.entity.Resource;
import com.booking.resourcebooking.entity.Role;
import com.booking.resourcebooking.entity.User;
import com.booking.resourcebooking.exception.InvalidRequestException;
import com.booking.resourcebooking.exception.ResourceNotFoundException;
import com.booking.resourcebooking.mapper.ReservationMapper;
import com.booking.resourcebooking.repository.ReservationRepository;
import com.booking.resourcebooking.repository.ReservationSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceService resourceService;

    @Transactional
    public ReservationResponse create(ReservationRequest request, User currentUser) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new InvalidRequestException("endTime must be after startTime");
        }

        Resource resource = resourceService.findEntityOrThrow(request.getResourceId());
        if (!resource.isAvailable()) {
            throw new InvalidRequestException("Resource '" + resource.getName() + "' is not available for booking");
        }

        // Reservation owner is always the authenticated JWT principal,
        // never a value supplied by the client in the request body.
        Reservation reservation = Reservation.builder()
                .resource(resource)
                .user(currentUser)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ReservationStatus.PENDING)
                .build();

        return ReservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id, User currentUser) {
        Reservation reservation = findEntityOrThrow(id);
        assertCanView(reservation, currentUser);
        return ReservationMapper.toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> list(ReservationStatus status,
                                           BigDecimal minPrice,
                                           BigDecimal maxPrice,
                                           Pageable pageable,
                                           User currentUser) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidRequestException("minPrice cannot be greater than maxPrice");
        }

        // ADMIN sees every reservation; USER is restricted to their own.
        Long ownerFilter = currentUser.getRole() == Role.ADMIN ? null : currentUser.getId();

        var spec = ReservationSpecification.build(ownerFilter, status, minPrice, maxPrice);
        return reservationRepository.findAll(spec, pageable).map(ReservationMapper::toResponse);
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationRequest request, User currentUser) {
        Reservation reservation = findEntityOrThrow(id);
        assertCanModify(reservation, currentUser);

        if (currentUser.getRole() != Role.ADMIN && reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidRequestException("Only PENDING reservations can be modified");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new InvalidRequestException("endTime must be after startTime");
        }

        Resource resource = resourceService.findEntityOrThrow(request.getResourceId());
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());

        return ReservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse updateStatus(Long id, ReservationStatusUpdateRequest request, User currentUser) {
        Reservation reservation = findEntityOrThrow(id);
        assertCanModify(reservation, currentUser);

        if (currentUser.getRole() != Role.ADMIN && request.getStatus() != ReservationStatus.CANCELLED) {
            throw new AccessDeniedException("Users may only cancel their own reservations");
        }

        reservation.setStatus(request.getStatus());
        return ReservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Reservation reservation = findEntityOrThrow(id);
        // Only ADMIN may hard-delete a reservation; USER should cancel instead.
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only administrators can delete reservations");
        }
        reservationRepository.delete(reservation);
    }

    private Reservation findEntityOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private void assertCanView(Reservation reservation, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You may only view your own reservations");
        }
    }

    private void assertCanModify(Reservation reservation, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You may only modify your own reservations");
        }
    }
}
