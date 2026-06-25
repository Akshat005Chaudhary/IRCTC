package ticket.bookings.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ticket.bookings.entities.User;
import ticket.bookings.services.UserBookingService;
import ticket.bookings.util.JwtService;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserBookingService bookingService;
    private final JwtService jwtService;

    @Autowired
    public UserController(UserBookingService bookingService, JwtService jwtService) {
        this.bookingService = bookingService;
        this.jwtService = jwtService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody User user) {
        boolean success = bookingService.signUpUser(user);
        if (success) {
            return ResponseEntity.ok("User registered successfully.");
        } else {
            return ResponseEntity.badRequest().body("Failed to register user. Email might already exist.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<User> userOpt = bookingService.loginUser(loginRequest.getEmail(), loginRequest.getPassword());
        if (userOpt.isPresent()) {
            String token = jwtService.generateToken(userOpt.get().getEmail());
            return ResponseEntity.ok(new LoginResponse(token));
        } else {
            return ResponseEntity.status(401).body("Invalid email or password.");
        }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private String token;

        public LoginResponse() {}

        public LoginResponse(String token) {
            this.token = token;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}
