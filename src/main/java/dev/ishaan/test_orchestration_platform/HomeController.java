package dev.ishaan.test_orchestration_platform;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Test Orchestration Platform API is running. See /swagger-ui.html for documentation.";
    }
}