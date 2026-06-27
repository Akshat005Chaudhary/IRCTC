package ticket.bookings.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import ticket.bookings.entities.*;
import ticket.bookings.repositories.*;
import ticket.bookings.util.UserServiceUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserBookingService {

    private final UserRepository userRepository;
    private final TrainRepository trainRepository;
    private final TicketRepository ticketRepository;

    @Autowired
    public UserBookingService(UserRepository userRepository, 
                              TrainRepository trainRepository, 
                              TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.trainRepository = trainRepository;
        this.ticketRepository = ticketRepository;
    }

    // Signup method
    @Transactional
    public boolean signUpUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            System.out.println("User with email " + user.getEmail() + " already exists.");
            return false;
        }
        
        String hashedPassword = UserServiceUtil.hashPassword(user.getPassword());
        user.setPassword(hashedPassword);
        
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            user.setUserId(UUID.randomUUID().toString());
        }
        if (user.getTicketsBooked() == null) {
            user.setTicketsBooked(new ArrayList<>());
        }

        userRepository.save(user);
        return true;
    }

    // Login method
    public Optional<User> loginUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (UserServiceUtil.checkPassword(password, user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    // Train Search
    @Cacheable(value = "trains", key = "#source + '-' + #destination")
    public List<Train> searchTrains(String source, String destination) {
        List<Train> allTrains = trainRepository.findAll();
        List<Train> foundTrains = new ArrayList<>();
        
        for (Train train : allTrains) {
            List<String> stations = train.getStations();
            int sourceIndex = stations.indexOf(source);
            int destIndex = stations.indexOf(destination);

            // Ensure both stations exist and source is before destination
            if (sourceIndex != -1 && destIndex != -1 && sourceIndex < destIndex) {
                foundTrains.add(train);
            }
        }
        return foundTrains;
    }

    // Book ticket
    @Transactional
    @CacheEvict(value = "trains", allEntries = true)
    public Ticket bookTicket(String userId, String trainId, String source, String destination, String dateOfTravel) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = userOpt.get();

        Optional<Train> trainOpt = trainRepository.findByIdForUpdate(trainId);
        if (trainOpt.isEmpty()) {
            throw new IllegalArgumentException("Train not found.");
        }
        Train train = trainOpt.get();

        List<List<Integer>> seats = train.getSeats();
        int bookedRow = -1;
        int bookedCol = -1;

        // Find the first available seat (0)
        for (int i = 0; i < seats.size(); i++) {
            List<Integer> row = seats.get(i);
            for (int j = 0; j < row.size(); j++) {
                if (row.get(j) == 0) {
                    row.set(j, 1); // Book the seat
                    bookedRow = i;
                    bookedCol = j;
                    break;
                }
            }
            if (bookedRow != -1) break;
        }

        if (bookedRow == -1) {
            throw new IllegalStateException("No seats available on this train.");
        }

        // Calculate the station hops covered
        List<String> stations = train.getStations();
        int sourceIndex = stations.indexOf(source);
        int destIndex = stations.indexOf(destination);
        int hops = (sourceIndex != -1 && destIndex != -1) ? (destIndex - sourceIndex) : 1;

        // Use JSR-354 / Moneta to calculate price (120 INR per station hop)
        javax.money.CurrencyUnit currencyUnit = javax.money.Monetary.getCurrency("INR");
        javax.money.MonetaryAmount baseRate = org.javamoney.moneta.Money.of(120.00, currencyUnit);
        javax.money.MonetaryAmount totalFare = baseRate.multiply(hops);

        java.math.BigDecimal finalPrice = totalFare.getNumber().numberValue(java.math.BigDecimal.class);
        String currencyCode = "INR";

        // Generate ticket details
        String ticketId = UUID.randomUUID().toString();
        String pnr = "PNR" + (int)(Math.random() * 900000000 + 100000000); // 9-digit random PNR
        String seatNo = "Row " + (bookedRow + 1) + ", Seat " + (bookedCol + 1);

        Ticket newTicket = new Ticket(ticketId, pnr, userId, trainId, source, destination, dateOfTravel, seatNo, finalPrice, currencyCode);


        // Save state changes
        trainRepository.save(train);
        ticketRepository.save(newTicket);
        
        user.getTicketsBooked().add(pnr);
        userRepository.save(user);

        System.out.println("Ticket Booked Successfully! PNR: " + pnr + ", Seat: " + seatNo);
        return newTicket;
    }

    // Cancel Ticket
    @Transactional
    @CacheEvict(value = "trains", allEntries = true)
    public boolean cancelTicket(String pnr, String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            System.out.println("User not found.");
            return false;
        }
        User user = userOpt.get();

        Optional<Ticket> ticketOpt = ticketRepository.findByPnr(pnr);
        if (ticketOpt.isEmpty() || !ticketOpt.get().getUserId().equals(userId)) {
            System.out.println("No ticket found with this PNR for the current user.");
            return false;
        }
        Ticket ticketToCancel = ticketOpt.get();

        // JSR-354 / Moneta refund logic: refund 75% (deduct 25% cancellation charge)
        javax.money.CurrencyUnit currencyUnit = javax.money.Monetary.getCurrency(ticketToCancel.getCurrency());
        javax.money.MonetaryAmount ticketPrice = org.javamoney.moneta.Money.of(ticketToCancel.getPrice(), currencyUnit);
        javax.money.MonetaryAmount refundAmount = ticketPrice.multiply(0.75);

        System.out.println("Processing refund of: " + refundAmount + " back to the user account.");

        // Release the seat on the train
        String trainId = ticketToCancel.getTrainId();
        String seatNo = ticketToCancel.getSeatNo();

        Optional<Train> trainOpt = trainRepository.findByIdForUpdate(trainId);
        if (trainOpt.isPresent()) {
            Train train = trainOpt.get();
            try {
                // Parse row and seat indexes from "Row X, Seat Y"
                String[] parts = seatNo.split(", ");
                int row = Integer.parseInt(parts[0].replace("Row ", "")) - 1;
                int col = Integer.parseInt(parts[1].replace("Seat ", "")) - 1;

                train.getSeats().get(row).set(col, 0); // Mark seat as available
                trainRepository.save(train);
            } catch (Exception e) {
                System.out.println("Error releasing seat: " + e.getMessage());
            }
        }

        // Remove the ticket and association
        ticketRepository.delete(ticketToCancel);
        user.getTicketsBooked().remove(pnr);
        userRepository.save(user);

        System.out.println("Ticket Cancelled Successfully.");
        return true;
    }

    // Fetching bookings
    public List<Ticket> fetchBookings(String userId) {
        return ticketRepository.findByUserId(userId);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
