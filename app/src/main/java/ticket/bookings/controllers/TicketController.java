package ticket.bookings.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ticket.bookings.entities.Ticket;
import ticket.bookings.entities.User;
import ticket.bookings.services.UserBookingService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final UserBookingService bookingService;

    @Autowired
    public TicketController(UserBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookTicket(Principal principal, @RequestBody BookRequest bookRequest) {
        if (principal == null) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("User not authenticated.");
        }
        String email = principal.getName();
        User user = bookingService.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        Ticket ticket = bookingService.bookTicket(
                user.getUserId(),
                bookRequest.getTrainId(),
                bookRequest.getSource(),
                bookRequest.getDestination(),
                bookRequest.getDateOfTravel()
        );
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelTicket(Principal principal, @RequestBody CancelRequest cancelRequest) {
        if (principal == null) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("User not authenticated.");
        }
        String email = principal.getName();
        User user = bookingService.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        boolean success = bookingService.cancelTicket(cancelRequest.getPnr(), user.getUserId());
        if (success) {
            return ResponseEntity.ok("Ticket cancelled successfully.");
        } else {
            throw new IllegalArgumentException("Failed to cancel ticket. Invalid PNR or user matching.");
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> fetchBookings(Principal principal) {
        if (principal == null) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("User not authenticated.");
        }
        String email = principal.getName();
        User user = bookingService.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        List<Ticket> tickets = bookingService.fetchBookings(user.getUserId());
        return ResponseEntity.ok(tickets);
    }

    public static class BookRequest {
        private String trainId;
        private String source;
        private String destination;
        private String dateOfTravel;

        public String getTrainId() { return trainId; }
        public void setTrainId(String trainId) { this.trainId = trainId; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getDateOfTravel() { return dateOfTravel; }
        public void setDateOfTravel(String dateOfTravel) { this.dateOfTravel = dateOfTravel; }
    }

    public static class CancelRequest {
        private String pnr;

        public String getPnr() { return pnr; }
        public void setPnr(String pnr) { this.pnr = pnr; }
    }
}
