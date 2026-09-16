package dev.ishaan.test_orchestration_platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserService userService;
    private final UserRepository userRepository;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD:password123}")
    private String adminPassword;

    public DataSeeder(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            userService.createUser(adminUsername, adminPassword, "ADMIN");
            System.out.println("Seeded test user: " + adminUsername + " (ADMIN role)");
        }
    }

}