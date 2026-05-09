package com.codearena.module7_coaching.config;

import com.codearena.module7_coaching.entity.Coach;
import com.codearena.module7_coaching.repository.CoachRepository;
import com.codearena.user.entity.AuthProvider;
import com.codearena.user.entity.Role;
import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class CoachSeederService implements CommandLineRunner {

    private final CoachRepository coachRepository;
    private final UserRepository userRepository;

    private static final List<CoachDef> COACHES = List.of(
            new CoachDef("JavaCoach@gmail.com", "Java", "Coach",
                    "Senior Java Developer with 10+ years of experience in Spring Boot, Microservices, and Cloud Architecture.",
                    Arrays.asList("JAVA", "DOTNET"), 4.8),
            new CoachDef("PythonCoach@gmail.com", "Python", "Coach",
                    "Python expert specializing in Data Science, Machine Learning, and Backend Development with Django/Flask.",
                    Arrays.asList("PYTHON", "JAVASCRIPT"), 4.6),
            new CoachDef("AngularCoach@gmail.com", "Angular", "Coach",
                    "Frontend specialist with deep expertise in Angular, TypeScript, RxJS, and modern CSS frameworks.",
                    Arrays.asList("ANGULAR", "JAVASCRIPT", "CSS"), 4.9));

    @Override
    public void run(String... args) {
        log.info("=== CoachSeederService: Ensuring coach accounts exist ===");
        for (CoachDef def : COACHES) {
            try {
                var existingUser = userRepository.findByEmail(def.email);
                User user;

                if (existingUser.isEmpty()) {
                    String localId = "local|" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                    user = User.builder()
                            .auth0Id(localId)
                            .email(def.email)
                            .firstName(def.firstName)
                            .lastName(def.lastName)
                            .role(Role.COACH)
                            .authProvider(AuthProvider.LOCAL)
                            .isActive(true)
                            .build();
                    userRepository.save(user);
                    log.info("Created local coach user: {}", def.email);
                } else {
                    user = existingUser.get();
                    if (user.getRole() != Role.COACH) {
                        user.setRole(Role.COACH);
                        userRepository.save(user);
                        log.info("Updated {} to COACH role", def.email);
                    }
                }

                if (coachRepository.findByUserId(user.getAuth0Id()).isEmpty()) {
                    Coach coach = Coach.builder()
                            .userId(user.getAuth0Id())
                            .bio(def.bio)
                            .specializations(def.specializations)
                            .rating(def.rating)
                            .totalSessions(0)
                            .build();
                    coachRepository.save(coach);
                    log.info("Created coach profile for {}", def.email);
                } else {
                    log.info("Coach profile already exists for {}", def.email);
                }
            } catch (Exception e) {
                log.error("Error seeding coach {}: {}", def.email, e.getMessage());
            }
        }
        log.info("=== CoachSeederService: Done ===");
    }

    private record CoachDef(
            String email,
            String firstName,
            String lastName,
            String bio,
            List<String> specializations,
            double rating) {
    }
}