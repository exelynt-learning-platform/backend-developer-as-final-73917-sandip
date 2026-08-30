package com.booking.resourcebooking.service;

import com.booking.resourcebooking.dto.reservation.ReservationRequest;
import com.booking.resourcebooking.entity.*;
import com.booking.resourcebooking.exception.InvalidRequestException;
import com.booking.resourcebooking.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private ReservationService reservationService;

    private User owner;
    private User otherUser;
    private User admin;
    private Resource resource;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("alice").role(Role.USER).build();
        otherUser = User.builder().id(2L).username("bob").role(Role.USER).build();
        admin = User.builder().id(3L).username("admin").role(Role.ADMIN).build();
        resource = Resource.builder().id(10L).name("Room").type("ROOM").available(true).build();
    }

    private ReservationRequest validRequest() {
        ReservationRequest req = new ReservationRequest();
        req.setResourceId(10L);
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        req.setPrice(new BigDecimal("40.00"));
        return req;
    }

    @Test
    void create_setsOwnerFromAuthenticatedUser_notFromRequest() {
        when(resourceService.findEntityOrThrow(10L)).thenReturn(resource);
        when(reservationRepository.save(any())).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        var response = reservationService.create(validRequest(), owner);

        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void create_withEndBeforeStart_throwsInvalidRequestException() {
        ReservationRequest req = validRequest();
        req.setStartTime(LocalDateTime.now().plusDays(2));
        req.setEndTime(LocalDateTime.now().plusDays(1));

        assertThatThrownBy(() -> reservationService.create(req, owner))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void create_onUnavailableResource_throwsInvalidRequestException() {
        resource.setAvailable(false);
        when(resourceService.findEntityOrThrow(10L)).thenReturn(resource);

        assertThatThrownBy(() -> reservationService.create(validRequest(), owner))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void getById_whenNotOwner_throwsAccessDenied() {
        Reservation reservation = Reservation.builder()
                .id(5L).user(owner).resource(resource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(BigDecimal.TEN).status(ReservationStatus.PENDING)
                .build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getById(5L, otherUser))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_asAdmin_canViewAnyReservation() {
        Reservation reservation = Reservation.builder()
                .id(5L).user(owner).resource(resource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(BigDecimal.TEN).status(ReservationStatus.PENDING)
                .build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        var response = reservationService.getById(5L, admin);

        assertThat(response.getId()).isEqualTo(5L);
    }

    @Test
    void updateStatus_userTryingToConfirm_throwsAccessDenied() {
        Reservation reservation = Reservation.builder()
                .id(5L).user(owner).resource(resource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(BigDecimal.TEN).status(ReservationStatus.PENDING)
                .build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        var statusReq = new com.booking.resourcebooking.dto.reservation.ReservationStatusUpdateRequest();
        statusReq.setStatus(ReservationStatus.CONFIRMED);

        assertThatThrownBy(() -> reservationService.updateStatus(5L, statusReq, owner))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateStatus_userCancellingOwnReservation_succeeds() {
        Reservation reservation = Reservation.builder()
                .id(5L).user(owner).resource(resource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(BigDecimal.TEN).status(ReservationStatus.PENDING)
                .build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var statusReq = new com.booking.resourcebooking.dto.reservation.ReservationStatusUpdateRequest();
        statusReq.setStatus(ReservationStatus.CANCELLED);

        var response = reservationService.updateStatus(5L, statusReq, owner);

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void delete_asNonAdmin_throwsAccessDenied() {
        Reservation reservation = Reservation.builder()
                .id(5L).user(owner).resource(resource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .price(BigDecimal.TEN).status(ReservationStatus.PENDING)
                .build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.delete(5L, owner))
                .isInstanceOf(AccessDeniedException.class);
    }
}
