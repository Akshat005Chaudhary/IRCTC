package ticket.bookings.entities;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User{
    private String name;
    private String password;
    @Column(unique = true, nullable = false)
    private String email;
    private String phone;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_tickets_booked",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "ticket_pnr")
    private List<String> ticketsBooked;
    @Id
    private String userId;

    public User(){
        this.name = "";
        this.email = "";
        this.phone = "";
        this.userId = "";
        this.password = "";
        this.ticketsBooked = new ArrayList<>();
    }

    public User(String name, String email, String phone, String userId, String password, List<String> ticketsBooked){
    this.name = name;
    this.email = email;
    this.phone = phone;
    this.userId = userId;
    this.password = password;
    this.ticketsBooked = ticketsBooked;
    }

    public void setUserId(String userId){
        this.userId = userId;
    }
    public void setPassword(String password){
        this.password = password;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public void setPhone(String phone){
        this.phone = phone;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setTicketsBooked(List<String> ticketsBooked){
        this.ticketsBooked = ticketsBooked;
    }
    
    public String getUserId(){
        return this.userId;
    }
    public String getPassword(){
        return this.password;
    }
    public String getEmail(){
        return this.email;
    }
    public String getPhone(){
        return this.phone;
    }
    public String getName(){
        return this.name;
    }
    public List<String> getTicketsBooked(){
        return this.ticketsBooked;
    }

} 