package dev.ishaan.test_orchestration_platform;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RateLimiterService rateLimiterService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil, RateLimiterService rateLimiterService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credentials,
                                                     HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        Bucket bucket = rateLimiterService.resolveBucket(clientIp);

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many login attempts. Please try again later."));
        }

        String username = credentials.get("username");
        String rawPassword = credentials.get("password");

        var userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty() || !passwordEncoder.matches(rawPassword, userOptional.get().getPassword())) {
            logger.warn("Failed login attempt for username: {} from IP: {}", username, clientIp);   // NEW
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }

        String role = userOptional.get().getRole();
        String token = jwtUtil.generateToken(username, role);
        return ResponseEntity.ok(Map.of("token", token));
    }

}