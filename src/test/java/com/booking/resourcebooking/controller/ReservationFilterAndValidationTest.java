package com.booking.resourcebooking.controller;

import com.booking.resourcebooking.dto.reservation.ReservationRequest;
import com.booking.resourcebooking.entity.Resource;
import com.booking.resourcebooking.entity.Role;
import com.booking.resourcebooking.entity.User;
import com.booking.resourcebooking.repository.ReservationRepository;
import com.booking.resourcebooking.repository.ResourceRepository;
import com.booking.resourcebooking.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationFilterAndValidationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Resource resource;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        reservationRepository.deleteAll();
        userRepository.deleteAll();
        resourceRepository.deleteAll();

        userRepository.save(User.builder()
                .username("carol").password(passwordEncoder.encode("Carol@123")).role(Role.USER).build());
        resource = resourceRepository.save(Resource.builder()
                .name("Room X").type("ROOM").available(true).build());

        var loginBody = objectMapper.writeValueAsString(
                new com.booking.resourcebooking.dto.auth.LoginRequest("carol", "Carol@123"));
        var result = mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andReturn();
        userToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        createReservation("30.00");
        createReservation("75.00");
        createReservation("120.00");
    }

    private void createReservation(String price) throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setResourceId(resource.getId());
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        req.setPrice(new BigDecimal(price));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void filterByMinAndMaxPrice_returnsOnlyMatchingReservations() throws Exception {
        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("minPrice", "50")
                        .param("maxPrice", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].price").value(75.00));
    }

    @Test
    void filterByStatus_returnsOnlyPendingReservations() throws Exception {
        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void pagination_returnsCorrectPageSize() throws Exception {
        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void sorting_byPriceDescending() throws Exception {
        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].price").value(120.00));
    }

    @Test
    void createReservation_withEndTimeBeforeStartTime_returns400() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setResourceId(resource.getId());
        req.setStartTime(LocalDateTime.now().plusDays(2));
        req.setEndTime(LocalDateTime.now().plusDays(1));
        req.setPrice(new BigDecimal("20.00"));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservation_withNegativePrice_returns400() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setResourceId(resource.getId());
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        req.setPrice(new BigDecimal("-5.00"));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void createReservation_withNonExistentResource_returns404() throws Exception {
        ReservationRequest req = new ReservationRequest();
        req.setResourceId(999999L);
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        req.setPrice(new BigDecimal("20.00"));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
