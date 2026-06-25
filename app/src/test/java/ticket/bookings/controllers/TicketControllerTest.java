package ticket.bookings.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ticket.bookings.entities.User;

import java.util.ArrayList;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
}
