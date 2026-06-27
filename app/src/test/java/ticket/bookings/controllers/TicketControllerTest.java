package ticket.bookings.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ticket.bookings.entities.Train;
import ticket.bookings.entities.User;
import ticket.bookings.repositories.TrainRepository;
import ticket.bookings.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.cache.type=simple")
@AutoConfigureMockMvc
public class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void testTicketLifecycle() throws Exception {
        String email = "ticket_test_" + System.currentTimeMillis() + "@example.com";
        User user = new User("Ticket User", email, "9876543210", "ticket_user", "password123", new ArrayList<>());

        // 1. Signup
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());

        // 2. Login to get JWT Token
        UserController.LoginRequest loginRequest = new UserController.LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andReturn();

        String responseString = loginResult.getResponse().getContentAsString();
        UserController.LoginResponse loginResponse = objectMapper.readValue(responseString, UserController.LoginResponse.class);
        String token = loginResponse.getToken();

        // 3. Book a ticket using JWT token
        TicketController.BookRequest bookRequest = new TicketController.BookRequest();
        bookRequest.setTrainId("T101");
        bookRequest.setSource("Delhi");
        bookRequest.setDestination("Kanpur");
        bookRequest.setDateOfTravel("2026-07-01");

        MvcResult bookResult = mockMvc.perform(post("/api/tickets/book")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value(notNullValue()))
                .andExpect(jsonPath("$.trainId").value("T101"))
                .andReturn();

        String ticketResponse = bookResult.getResponse().getContentAsString();
        // Parse PNR
        String pnr = objectMapper.readTree(ticketResponse).get("pnr").asText();

        // 4. Fetch bookings using JWT token
        mockMvc.perform(get("/api/tickets/bookings")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pnr").value(pnr));

        // 5. Cancel ticket using JWT token
        TicketController.CancelRequest cancelRequest = new TicketController.CancelRequest();
        cancelRequest.setPnr(pnr);

        mockMvc.perform(post("/api/tickets/cancel")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk());

        // 6. Fetch bookings again using JWT token (should be empty now)
        mockMvc.perform(get("/api/tickets/bookings")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testTicketErrorHandling() throws Exception {
        String email = "err_test_" + System.currentTimeMillis() + "@example.com";
        User user = new User("Err User", email, "9876543211", "err_user", "password123", new ArrayList<>());

        // Signup and Login
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());

        UserController.LoginRequest loginRequest = new UserController.LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = loginResult.getResponse().getContentAsString();
        UserController.LoginResponse loginResponse = objectMapper.readValue(responseString, UserController.LoginResponse.class);
        String token = loginResponse.getToken();

        // Perform booking with invalid train ID
        TicketController.BookRequest bookRequest = new TicketController.BookRequest();
        bookRequest.setTrainId("INVALID_TRAIN");
        bookRequest.setSource("Delhi");
        bookRequest.setDestination("Kanpur");
        bookRequest.setDateOfTravel("2026-07-01");

        mockMvc.perform(post("/api/tickets/book")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Train not found."))
                .andExpect(jsonPath("$.path").value("/api/tickets/book"));
    }

    @Test
    public void testConcurrentBooking() throws Exception {
        // 1. Seed a test train with exactly 1 available seat
        String trainId = "CONC_TRAIN_" + System.currentTimeMillis();
        List<String> stations = Arrays.asList("Delhi", "Kanpur");
        Map<String, String> stationTimes = new HashMap<>();
        stationTimes.put("Delhi", "10:00");
        stationTimes.put("Kanpur", "15:00");
        
        // 1 row with 1 seat, initialized to 0 (available)
        List<List<Integer>> seats = new ArrayList<>();
        List<Integer> row = new ArrayList<>();
        row.add(0);
        seats.add(row);
        
        Train train = new Train(trainId, "12345", stations, stationTimes, seats);
        trainRepository.save(train);

        // 2. Register 5 users and get their JWT tokens
        int numThreads = 5;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            String email = "conc_user_" + i + "_" + System.currentTimeMillis() + "@example.com";
            User user = new User("User " + i, email, "987654321" + i, "user_" + i, "password", new ArrayList<>());
            
            // Signup
            mockMvc.perform(post("/api/users/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user)))
                    .andExpect(status().isOk());
            
            // Login
            UserController.LoginRequest loginReq = new UserController.LoginRequest();
            loginReq.setEmail(email);
            loginReq.setPassword("password");
            
            MvcResult loginRes = mockMvc.perform(post("/api/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andReturn();
            
            String token = objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("token").asText();
            tokens.add(token);
        }

        // 3. Perform 5 concurrent bookings
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final String token = tokens.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait for the start signal
                    
                    TicketController.BookRequest bookReq = new TicketController.BookRequest();
                    bookReq.setTrainId(trainId);
                    bookReq.setSource("Delhi");
                    bookReq.setDestination("Kanpur");
                    bookReq.setDateOfTravel("2026-07-01");
                    
                    MvcResult res = mockMvc.perform(post("/api/tickets/book")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bookReq)))
                            .andReturn();
                    
                    int status = res.getResponse().getStatus();
                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // start all threads at once
        endLatch.await(); // wait for all threads to complete
        executor.shutdown();

        // 4. Assertions
        Assertions.assertEquals(1, successCount.get(), "Exactly one booking must succeed.");
        Assertions.assertEquals(numThreads - 1, failureCount.get(), "Remaining bookings must fail.");

        // Verify database state: seat should be marked as 1
        Train updatedTrain = trainRepository.findById(trainId).orElseThrow();
        Assertions.assertEquals(1, updatedTrain.getSeats().get(0).get(0), "Seat must be booked.");
    }

    @Test
    public void testCachingAndEviction() throws Exception {
        // Clear any existing cache entries
        if (cacheManager.getCache("trains") != null) {
            cacheManager.getCache("trains").clear();
        }

        // 1. Seed a test train
        String trainId = "CACHE_TRAIN_" + System.currentTimeMillis();
        List<String> stations = Arrays.asList("Delhi", "Kanpur");
        Map<String, String> stationTimes = new HashMap<>();
        stationTimes.put("Delhi", "10:00");
        stationTimes.put("Kanpur", "15:00");
        
        List<List<Integer>> seats = new ArrayList<>();
        List<Integer> row = new ArrayList<>();
        row.add(0);
        seats.add(row);
        
        Train train = new Train(trainId, "12345", stations, stationTimes, seats);
        trainRepository.save(train);

        // 2. Perform train search to populate the cache
        mockMvc.perform(get("/api/trains/search")
                .param("source", "Delhi")
                .param("destination", "Kanpur"))
                .andExpect(status().isOk());

        // Verify cache is populated
        org.springframework.cache.Cache cache = cacheManager.getCache("trains");
        Assertions.assertNotNull(cache);
        Object cachedValue = cache.get("Delhi-Kanpur");
        Assertions.assertNotNull(cachedValue, "Search query Delhi-Kanpur must be cached.");

        // 3. Register a user and book ticket to evict cache
        String email = "cache_user_" + System.currentTimeMillis() + "@example.com";
        User user = new User("Cache User", email, "9876543219", "cache_user", "password", new ArrayList<>());
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());

        UserController.LoginRequest loginReq = new UserController.LoginRequest();
        loginReq.setEmail(email);
        loginReq.setPassword("password");
        MvcResult loginRes = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("token").asText();

        TicketController.BookRequest bookReq = new TicketController.BookRequest();
        bookReq.setTrainId(trainId);
        bookReq.setSource("Delhi");
        bookReq.setDestination("Kanpur");
        bookReq.setDateOfTravel("2026-07-01");

        mockMvc.perform(post("/api/tickets/book")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookReq)))
                .andExpect(status().isOk());

        // Verify cache is evicted
        cachedValue = cache.get("Delhi-Kanpur");
        Assertions.assertNull(cachedValue, "Cache must be evicted after booking a ticket.");
    }
}
