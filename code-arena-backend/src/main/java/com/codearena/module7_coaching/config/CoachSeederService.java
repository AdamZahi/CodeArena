package com.codearena.module7_coaching.config;

import com.codearena.module7_coaching.entity.Coach;
import com.codearena.module7_coaching.repository.CoachRepository;
import com.codearena.user.service.impl.Auth0ManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

import com.codearena.user.entity.AuthProvider;
import com.codearena.user.entity.Role;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;

import java.util.Arrays;
import java.util.List;

/**
 * Seeds 3 real coach users in Auth0 + links them in the local coaches table.
 * Runs BEFORE CoachingDataInitializer (Order=5) so the coach IDs are ready.
 *
 * Coaches created:
 * JavaCoach@gmail.com / Hazem007* → role COACH → userId stored in coaches table
 * PythonCoach@gmail.com / Hazem007* → role COACH
 * AngularCoach@gmail.com/ Hazem007* → role COACH
 */
@Slf4j
// @Component
// @Order(5)
@RequiredArgsConstructor
public class CoachSeederService implements CommandLineRunner {

    private final Auth0ManagementService auth0ManagementService;
    private final CoachRepository coachRepository;
    private final UserRepository userRepository;

    private static final String PASSWORD = "Hazem007*";

    private static final List<CoachDef> COACHES = List.of(
            new CoachDef("JavaCoach@gmail.com", "Java Coach",
                    "Senior Java Developer with 10+ years of experience in Spring Boot, Microservices, and Cloud Architecture.",
                    Arrays.asList("JAVA", "DOTNET"), 4.8),
            new CoachDef("PythonCoach@gmail.com", "Python Coach",
                    "Python expert specializing in Data Science, Machine Learning, and Backend Development with Django/Flask.",
                    Arrays.asList("PYTHON", "JAVASCRIPT"), 4.6),
            new CoachDef("AngularCoach@gmail.com", "Angular Coach",
                    "Frontend specialist with deep expertise in Angular, TypeScript, RxJS, and modern CSS frameworks.",
                    Arrays.asList("ANGULAR", "JAVASCRIPT", "CSS"), 4.9));

    @Override
    public void run(String... args) {
        log.info("=== CoachSeederService: Ensuring coach accounts exist ===");
        for (CoachDef def : COACHES) {
            try {
                // Create (or retrieve existing) Auth0 user
                String auth0UserId = auth0ManagementService.createUser(def.email, PASSWORD, def.name);
                if (auth0UserId == null) {
                    log.warn("Could not resolve Auth0 userid for {}, skipping link", def.email);
                    continue;
                }

                // Assign COACH role in Auth0
                auth0ManagementService.assignRoleToUser(auth0UserId, "COACH");

                // Save into local users table so they are recognized as COACH
                var userOpt = userRepository.findByKeycloakId(auth0UserId);
                if (userOpt.isEmpty()) {
                    userRepository.save(User.builder()
                            .keycloakId(auth0UserId)
                            .email(def.email)
                            .firstName(def.name.split(" ")[0])
                            .lastName(def.name.split(" ").length > 1 ? def.name.split(" ")[1] : "")
                            .role(Role.COACH)
                            .authProvider(AuthProvider.LOCAL)
                            .isActive(true)
                            .build());
                    log.info("Created local users profile for {} ({})", def.email, auth0UserId);
                } else {
                    User existingUser = userOpt.get();
                    if (existingUser.getRole() != Role.COACH) {
                        existingUser.setRole(Role.COACH);
                        userRepository.save(existingUser);
                        log.info("Updated existing user profile for {} to COACH", def.email);
                    }
                }

                // Upsert coach profile in local DB
                if (coachRepository.findByUserId(auth0UserId).isEmpty()) {
                    Coach coach = Coach.builder()
                            .userId(auth0UserId)
                            .bio(def.bio)
                            .specializations(def.specializations)
                            .rating(def.rating)
                            .totalSessions(0)
                            .build();
                    coachRepository.save(coach);
                    log.info("Created local coach profile for {} ({})", def.email, auth0UserId);
                } else {
                    log.info("Coach profile already exists for {} ({})", def.email, auth0UserId);
                }
            } catch (Exception e) {
                log.error("Error seeding coach {}: {}", def.email, e.getMessage());
            }
        }
        log.info("=== CoachSeederService: Done ===");
    }

    private record CoachDef(
            String email,
            String name,
            String bio,
            List<String> specializations,
            double rating) {
    }
}
