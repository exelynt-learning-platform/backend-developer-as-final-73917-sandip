package com.booking.resourcebooking.dto.reservation;

import com.booking.resourcebooking.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatusUpdateRequest {

    @NotNull(message = "status is required")
    private ReservationStatus status;
}
