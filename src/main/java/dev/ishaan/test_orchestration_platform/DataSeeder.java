package dev.ishaan.test_orchestration_platform;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserService userService;
    private final UserRepository userRepository;

    public DataSeeder(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {           // only create if it doesn't already exist
            userService.createUser("admin", "password123");
            System.out.println("Seeded test user: admin / password123");
        }
    }

}