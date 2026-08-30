package com.booking.resourcebooking.config;

import com.booking.resourcebooking.entity.Resource;
import com.booking.resourcebooking.entity.Role;
import com.booking.resourcebooking.entity.User;
import com.booking.resourcebooking.repository.ResourceRepository;
import com.booking.resourcebooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default ADMIN and USER account (for grading/testing) and a
 * handful of sample resources, but only if the tables are empty and
 * seeding is enabled via app.seed.enabled.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.admin-username}")
    private String adminUsername;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.user-username}")
    private String userUsername;

    @Value("${app.seed.user-password}")
    private String userPassword;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        if (userRepository.count() == 0) {
            userRepository.save(User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build());

            userRepository.save(User.builder()
                    .username(userUsername)
                    .password(passwordEncoder.encode(userPassword))
                    .role(Role.USER)
                    .build());

            log.info("Seeded default users -> ADMIN: '{}' / USER: '{}' (see README for passwords)",
                    adminUsername, userUsername);
        }

        if (resourceRepository.count() == 0) {
            resourceRepository.save(Resource.builder()
                    .name("Conference Room A")
                    .type("ROOM")
                    .description("Large conference room with projector, seats 12")
                    .available(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Toyota Camry - Fleet Car 1")
                    .type("VEHICLE")
                    .description("Company sedan for local trips")
                    .available(true)
                    .build());

            resourceRepository.save(Resource.builder()
                    .name("Portable Projector")
                    .type("EQUIPMENT")
                    .description("HD projector with HDMI and wireless casting")
                    .available(true)
                    .build());

            log.info("Seeded sample resources");
        }
    }
}
