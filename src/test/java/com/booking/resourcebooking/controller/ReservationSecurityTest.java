package com.booking.resourcebooking.controller;

import com.booking.resourcebooking.dto.reservation.ReservationRequest;
import com.booking.resourcebooking.entity.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end security tests covering: JWT-protected endpoints, RBAC
 * between ADMIN and USER roles, and that a USER can never see or
 * modify another USER's reservations, regardless of what is passed
 * in the request body.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationSecurityTest {

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

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        userRepository.deleteAll();
        resourceRepository.deleteAll();

        userRepository.save(User.builder()
                .username("admin1").password(passwordEncoder.encode("Admin@123")).role(Role.ADMIN).build());
        userRepository.save(User.builder()
                .username("alice").password(passwordEncoder.encode("Alice@123")).role(Role.USER).build());
        userRepository.save(User.builder()
                .username("bob").password(passwordEncoder.encode("Bob@123")).role(Role.USER).build());

        resource = resourceRepository.save(Resource.builder()
                .name("Test Room").type("ROOM").description("desc").available(true).build());
    }

    private String tokenFor(String username, String password) throws Exception {
        var body = objectMapper.writeValueAsString(new com.booking.resourcebooking.dto.auth.LoginRequest(username, password));
        var result = mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn();
        var json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private ReservationRequest sampleRequest() {
        ReservationRequest req = new ReservationRequest();
        req.setResourceId(resource.getId());
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        req.setPrice(new BigDecimal("50.00"));
        return req;
    }

    @Test
    void user_canCreateReservation_ownedByThemselves() throws Exception {
        String aliceToken = tokenFor("alice", "Alice@123");

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void user_cannotViewAnotherUsersReservation() throws Exception {
        String aliceToken = tokenFor("alice", "Alice@123");
        String bobToken = tokenFor("bob", "Bob@123");

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(response).get("id").asLong();

        // Bob tries to view Alice's reservation directly by id -> forbidden
        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_listReservations_onlyShowsOwnReservations() throws Exception {
        String aliceToken = tokenFor("alice", "Alice@123");
        String bobToken = tokenFor("bob", "Bob@123");

        mockMvc.perform(post("/reservations").header("Authorization", "Bearer " + aliceToken)
                .contentType("application/json").content(objectMapper.writeValueAsString(sampleRequest())));
        mockMvc.perform(post("/reservations").header("Authorization", "Bearer " + bobToken)
                .contentType("application/json").content(objectMapper.writeValueAsString(sampleRequest())));

        mockMvc.perform(get("/reservations").header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value("alice"));
    }

    @Test
    void admin_listReservations_seesAllUsersReservations() throws Exception {
        String adminToken = tokenFor("admin1", "Admin@123");
        String aliceToken = tokenFor("alice", "Alice@123");
        String bobToken = tokenFor("bob", "Bob@123");

        mockMvc.perform(post("/reservations").header("Authorization", "Bearer " + aliceToken)
                .contentType("application/json").content(objectMapper.writeValueAsString(sampleRequest())));
        mockMvc.perform(post("/reservations").header("Authorization", "Bearer " + bobToken)
                .contentType("application/json").content(objectMapper.writeValueAsString(sampleRequest())));

        mockMvc.perform(get("/reservations").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void user_cannotCreateOrModifyResources() throws Exception {
        String aliceToken = tokenFor("alice", "Alice@123");

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType("application/json")
                        .content("{\"name\":\"New Room\",\"type\":\"ROOM\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canCreateResources() throws Exception {
        String adminToken = tokenFor("admin1", "Admin@123");

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"name\":\"New Room\",\"type\":\"ROOM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Room"));
    }

    @Test
    void reservationOwner_isTakenFromJwt_notFromRequestBody() throws Exception {
        // Even if a malicious client tried to inject another user's id,
        // ReservationRequest has no such field, so it cannot be spoofed.
        String bobToken = tokenFor("bob", "Bob@123");

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    void user_cannotDeleteReservations() throws Exception {
        String aliceToken = tokenFor("alice", "Alice@123");

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andReturn().getResponse().getContentAsString();
        long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidJwt_isRejectedWith401() throws Exception {
        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }
}
