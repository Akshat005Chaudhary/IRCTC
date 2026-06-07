package ticket.bookings.services;

import ticket.bookings.entities.*;
import ticket.bookings.util.UserServiceUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserBookingService {

    // Fields
    private User user;
    private List<User> userList;
    private List<Train> trainList;
    private List<Ticket> ticketList;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final String USERS_PATH = "src/main/java/ticket/localDb/users.json";
    private static final String TRAINS_PATH = "src/main/java/ticket/localDb/trains.json";
    private static final String BOOKINGS_PATH = "src/main/java/ticket/localDb/bookings.json";

    // default constructor
    public UserBookingService() throws IOException{
        /*
        (Input/Output Exception) is used in this constructor because the code is actively reading data from external files stored on your hard drive.
        */
       /*
        Whenever there is contact with I/O it is always safe to put IOException there
       */
        File usersFile = new File(USERS_PATH);
        userList = objectMapper.readValue(usersFile, new TypeReference<List<User>>() {});
        File trainsFile = new File(TRAINS_PATH);
        trainList = objectMapper.readValue(trainsFile, new TypeReference<List<Train>>() {});
        File ticketsFile = new File(BOOKINGS_PATH);
        ticketList = objectMapper.readValue(ticketsFile, new TypeReference<List<Ticket>>() {});
    }

    // Helper method for saving users to user.json file
    public void saveUserListToFile() throws IOException {
        File usersFile = new File(USERS_PATH);
        objectMapper.writeValue(usersFile, userList);
    }

    public void saveTrainListToFile() throws IOException {
        File trainsFile = new File(TRAINS_PATH);
        objectMapper.writeValue(trainsFile, trainList);
    }

    public void saveTicketListToFile() throws IOException {
        File ticketsFile = new File(BOOKINGS_PATH);
        objectMapper.writeValue(ticketsFile, ticketList);
    }

    // Signup method
    public boolean signUpUser(User user){
        String hashedPassword = UserServiceUtil.hashPassword(user.getPassword());
        user.setPassword(hashedPassword);
        userList.add(user);
        try{
            saveUserListToFile();
            return true;
        }catch(IOException e){
            System.out.println("Error Occured:" + e.getMessage());
            return false;
        }
    }

    // login method
    public boolean loginUser(String email, String password){
        for(User user: userList){ 
            if(user.getEmail().equals(email)){
                if(UserServiceUtil.checkPassword(password, user.getPassword())){
                    this.user = user;
                    return true;
                }
            }
        }
        return false;
    }

    // Train Search
        public List<Train> searchTrains(String source, String destination) {
        List<Train> foundTrains = new ArrayList<>();
        for (Train train : trainList) {
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

    // Implementing booking ticket
    public boolean bookTicket(Train train, String source, String destination, String dateOfTravel) {
        if (user == null) {
            System.out.println("Please login to book a ticket.");
            return false;
        }

        List<List<Integer>> seats = train.getSeats();
        int bookedRow = -1;
        int bookedCol = -1;

        // Simple seat allocation: find the first 0 (available seat)
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
            System.out.println("No seats available on this train.");
            return false;
        }

        // Generate ticket details
        String ticketId = java.util.UUID.randomUUID().toString();
        String pnr = "PNR" + (int)(Math.random() * 900000000 + 100000000); // 9-digit random PNR
        String seatNo = "Row " + (bookedRow + 1) + ", Seat " + (bookedCol + 1);

        Ticket newTicket = new Ticket(ticketId, pnr, user.getUserId(), train.getTrainId(), source, destination, dateOfTravel, seatNo);

        ticketList.add(newTicket);
        user.getTicketsBooked().add(pnr);

        try {
            saveTrainListToFile();
            saveTicketListToFile();
            saveUserListToFile();
            System.out.println("Ticket Booked Successfully! PNR: " + pnr + ", Seat: " + seatNo);
            return true;
        } catch (IOException e) {
            System.out.println("Failed to save booking: " + e.getMessage());
            return false;
        }
    }

    // Cancelling Ticket
        public boolean cancelTicket(String pnr) {
        if (user == null) {
            System.out.println("Please login first.");
            return false;
        }

        Ticket ticketToCancel = null;
        for (Ticket t : ticketList) {
            if (t.getPnr().equals(pnr) && t.getUserId().equals(user.getUserId())) {
                ticketToCancel = t;
                break;
            }
        }

        if (ticketToCancel == null) {
            System.out.println("No ticket found with this PNR.");
            return false;
        }

        // Release the seat on the train
        String trainId = ticketToCancel.getTrainId();
        String seatNo = ticketToCancel.getSeatNo();

        try {
            // Parse row and seat indexes from "Row X, Seat Y"
            String[] parts = seatNo.split(", ");
            int row = Integer.parseInt(parts[0].replace("Row ", "")) - 1;
            int col = Integer.parseInt(parts[1].replace("Seat ", "")) - 1;

            for (Train train : trainList) {
                if (train.getTrainId().equals(trainId)) {
                    train.getSeats().get(row).set(col, 0); // Mark seat as available
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error releasing seat: " + e.getMessage());
        }

        // Remove the ticket
        ticketList.remove(ticketToCancel);
        user.getTicketsBooked().remove(pnr);

        try {
            saveTrainListToFile();
            saveTicketListToFile();
            saveUserListToFile();
            System.out.println("Ticket Cancelled Successfully.");
            return true;
        } catch (IOException e) {
            System.out.println("Failed to save cancellation: " + e.getMessage());
            return false;
        }
    }

    // Fetching bookings
    public void fetchBookings() {
        if (user == null) {
            System.out.println("Please login first.");
            return;
        }

        boolean hasBookings = false;
        for (Ticket ticket : ticketList) {
            if (ticket.getUserId().equals(user.getUserId())) {
                System.out.println("----------------------------------------");
                System.out.println("PNR: " + ticket.getPnr());
                System.out.println("Train ID: " + ticket.getTrainId());
                System.out.println("Route: " + ticket.getSource() + " -> " + ticket.getDestination());
                System.out.println("Date: " + ticket.getDateOfTravel());
                System.out.println("Seat: " + ticket.getSeatNo());
                hasBookings = true;
            }
        }

        if (!hasBookings) {
            System.out.println("No bookings found for the current user.");
        } else {
            System.out.println("----------------------------------------");
        }
    }

    // Getters and Setters for CLI interaction
    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Train> getTrainList() {
        return this.trainList;
    }


}
