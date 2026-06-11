package ticket.bookings.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ticket.bookings.entities.Train;
import ticket.bookings.services.UserBookingService;

import java.util.List;

@RestController
@RequestMapping("/api/trains")
public class TrainController {

    private final UserBookingService bookingService;

    @Autowired
    public TrainController(UserBookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<Train>> searchTrains(
            @RequestParam String source,
            @RequestParam String destination) {
        List<Train> trains = bookingService.searchTrains(source, destination);
        return ResponseEntity.ok(trains);
    }
}
