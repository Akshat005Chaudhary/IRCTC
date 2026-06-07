package ticket.bookings;

import ticket.bookings.entities.Train;
import ticket.bookings.entities.User;
import ticket.bookings.services.UserBookingService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class App {
    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("     Welcome to RailConnect CLI Engine   ");
        System.out.println("=========================================");

        UserBookingService bookingService;
        try {
            bookingService = new UserBookingService();
        } catch (IOException e) {
            System.out.println("System Error: Failed to initialize database: " + e.getMessage());
            return;
        }

        Scanner scanner = new Scanner(System.in);
        int choice = 0;

        while (choice != 3) {
            // Check if a user is logged in
            User currentUser = bookingService.getUser();

            if (currentUser == null) {
                // Landing Menu (Not Logged In)
                System.out.println("\n1. Sign Up");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Choose an option: ");
                
                if (!scanner.hasNextInt()) {
                    System.out.println("Invalid input. Please enter a number.");
                    scanner.next(); // consume invalid input
                    continue;
                }
                choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                switch (choice) {
                    case 1:
                        System.out.print("Enter name: ");
                        String name = scanner.nextLine();
                        System.out.print("Enter email: ");
                        String email = scanner.nextLine();
                        System.out.print("Enter phone: ");
                        String phone = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String password = scanner.nextLine();
                        
                        String userId = UUID.randomUUID().toString();
                        User newUser = new User(name, email, phone, userId, password, new ArrayList<>());
                        
                        if (bookingService.signUpUser(newUser)) {
                            System.out.println("Registration Successful! You can now Login.");
                        } else {
                            System.out.println("Registration Failed. Try again.");
                        }
                        break;
                    case 2:
                        System.out.print("Enter email: ");
                        String loginEmail = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String loginPassword = scanner.nextLine();
                        
                        if (bookingService.loginUser(loginEmail, loginPassword)) {
                            System.out.println("Login Successful! Welcome, " + bookingService.getUser().getName());
                        } else {
                            System.out.println("Invalid email or password.");
                        }
                        break;
                    case 3:
                        System.out.println("Thank you for using RailConnect!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please choose between 1 and 3.");
                }
            } else {
                // Passenger Dashboard (Logged In)
                System.out.println("\n--- Dashboard (" + currentUser.getName() + ") ---");
                System.out.println("1. Search Trains");
                System.out.println("2. Book a Ticket");
                System.out.println("3. View Bookings");
                System.out.println("4. Cancel a Ticket");
                System.out.println("5. Logout");
                System.out.print("Choose an option: ");

                if (!scanner.hasNextInt()) {
                    System.out.println("Invalid input. Please enter a number.");
                    scanner.next();
                    continue;
                }
                int dashboardChoice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                switch (dashboardChoice) {
                    case 1:
                        System.out.print("Enter source station: ");
                        String src = scanner.nextLine();
                        System.out.print("Enter destination station: ");
                        String dest = scanner.nextLine();
                        
                        List<Train> trains = bookingService.searchTrains(src, dest);
                        if (trains.isEmpty()) {
                            System.out.println("No trains found on this route.");
                        } else {
                            System.out.println("\nAvailable Trains:");
                            for (Train train : trains) {
                                System.out.println("Train No: " + train.getTrainNo() + " | ID: " + train.getTrainId() + " | Departs: " + train.getStationTimes().get(src));
                            }
                        }
                        break;

                    case 2:
                        System.out.print("Enter train ID to book: ");
                        String trainId = scanner.nextLine();
                        System.out.print("Enter source: ");
                        String bookSrc = scanner.nextLine();
                        System.out.print("Enter destination: ");
                        String bookDest = scanner.nextLine();
                        System.out.print("Enter date of travel (DD-MM-YYYY): ");
                        String date = scanner.nextLine();

                        // Find the train in our service
                        Train selectedTrain = null;
                        for (Train t : bookingService.getTrainList()) {
                            if (t.getTrainId().equalsIgnoreCase(trainId)) {
                                selectedTrain = t;
                                break;
                            }
                        }

                        if (selectedTrain == null) {
                            System.out.println("Invalid Train ID.");
                        } else {
                            bookingService.bookTicket(selectedTrain, bookSrc, bookDest, date);
                        }
                        break;

                    case 3:
                        bookingService.fetchBookings();
                        break;

                    case 4:
                        System.out.print("Enter PNR to cancel: ");
                        String pnrToCancel = scanner.nextLine();
                        bookingService.cancelTicket(pnrToCancel);
                        break;

                    case 5:
                        bookingService.setUser(null); // Logout
                        System.out.println("Logged out successfully.");
                        break;

                    default:
                        System.out.println("Invalid option.");
                }
            }
        }
        scanner.close();
    }
}
