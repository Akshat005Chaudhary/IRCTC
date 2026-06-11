package ticket.bookings.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ticket.bookings.entities.Ticket;
import ticket.bookings.services.UserBookingService;

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
    public ResponseEntity<?> bookTicket(@RequestBody BookRequest bookRequest) {
        try {
            Ticket ticket = bookingService.bookTicket(
                    bookRequest.getUserId(),
                    bookRequest.getTrainId(),
                    bookRequest.getSource(),
                    bookRequest.getDestination(),
                    bookRequest.getDateOfTravel()
            );
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelTicket(@RequestBody CancelRequest cancelRequest) {
        boolean success = bookingService.cancelTicket(cancelRequest.getPnr(), cancelRequest.getUserId());
        if (success) {
            return ResponseEntity.ok("Ticket cancelled successfully.");
        } else {
            return ResponseEntity.badRequest().body("Failed to cancel ticket. Invalid PNR or user matching.");
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Ticket>> fetchBookings(@PathVariable String userId) {
        List<Ticket> tickets = bookingService.fetchBookings(userId);
        return ResponseEntity.ok(tickets);
    }

    public static class BookRequest {
        private String userId;
        private String trainId;
        private String source;
        private String destination;
        private String dateOfTravel;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
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
        private String userId;

        public String getPnr() { return pnr; }
        public void setPnr(String pnr) { this.pnr = pnr; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
