package com.habitspark.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/auth")
public class AuthController {
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        // TODO: implement auth logic
        return ResponseEntity.ok();
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok();
    }
}
class LoginRequest {
    private String username; private String password;
}
class RegisterRequest {
    private Long familyGroupId; private String username; private String password; private String role; private String displayName;
}
