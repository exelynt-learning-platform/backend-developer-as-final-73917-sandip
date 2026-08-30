package com.booking.resourcebooking.repository;

import com.booking.resourcebooking.entity.Reservation;
import com.booking.resourcebooking.entity.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Builds dynamic JPA Specifications for filtering reservations by
 * owner (for USER role), status, and min/max price.
 */
public final class ReservationSpecification {

    private ReservationSpecification() {
    }

    public static Specification<Reservation> build(Long ownerUserId,
                                                     ReservationStatus status,
                                                     BigDecimal minPrice,
                                                     BigDecimal maxPrice) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (ownerUserId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("user").get("id"), ownerUserId));
            }
            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return predicates;
        };
    }
}
