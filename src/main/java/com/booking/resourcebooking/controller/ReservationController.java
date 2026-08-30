package com.booking.resourcebooking.controller;

import com.booking.resourcebooking.dto.common.PageResponse;
import com.booking.resourcebooking.dto.reservation.ReservationRequest;
import com.booking.resourcebooking.dto.reservation.ReservationResponse;
import com.booking.resourcebooking.dto.reservation.ReservationStatusUpdateRequest;
import com.booking.resourcebooking.entity.ReservationStatus;
import com.booking.resourcebooking.entity.User;
import com.booking.resourcebooking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservations", description = "Bookings made against resources")
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    @Operation(summary = "List reservations with filtering, pagination and sorting. " +
            "ADMIN sees all reservations, USER sees only their own.")
    public ResponseEntity<PageResponse<ReservationResponse>> list(
            @Parameter(description = "Filter by reservation status") @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum price filter") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter") @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        var result = reservationService.list(status, minPrice, maxPrice, pageable, currentUser);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single reservation by id (owner or ADMIN only)")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id,
                                                        @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reservationService.getById(id, currentUser));
    }

    @PostMapping
    @Operation(summary = "Create a reservation. Owner is always taken from the JWT, never the request body.")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request,
                                                       @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.create(request, currentUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a reservation's details. ADMIN can edit any; USER only their own PENDING reservations.")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody ReservationRequest request,
                                                       @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reservationService.update(id, request, currentUser));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a reservation's status. ADMIN may set any status; USER may only cancel their own.")
    public ResponseEntity<ReservationResponse> updateStatus(@PathVariable Long id,
                                                             @Valid @RequestBody ReservationStatusUpdateRequest request,
                                                             @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reservationService.updateStatus(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a reservation - ADMIN only")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        reservationService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
