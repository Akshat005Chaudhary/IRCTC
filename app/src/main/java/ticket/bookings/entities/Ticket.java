package ticket.bookings.entities;

public class Ticket{
    private String ticketId;
    private String pnr;
    private String userId;
    private String trainId;
    private String source;
    private String destination;
    private String dateOfTravel;
    private String seatNo;

    public Ticket(){
        this.ticketId = "";
        this.pnr = "";
        this.userId = "";
        this.trainId = "";
        this.source = "";
        this.destination = "";
        this.dateOfTravel = "";
        this.seatNo = "";
    }

    public Ticket(String ticketId, String pnr, String userId, String trainId, String source, String destination, String dateOfTravel, String seatNo){
        this.ticketId = ticketId;
        this.pnr = pnr;
        this.userId = userId;
        this.trainId = trainId;
        this.source = source;
        this.destination = destination;
        this.dateOfTravel = dateOfTravel;
        this.seatNo = seatNo;
    }

    public void setTicketId(String ticketId){
        this.ticketId = ticketId;
    }

    public void setPnr(String pnr){
        this.pnr = pnr;
    }

    public void setUserId(String userId){
        this.userId = userId;
    }

    public void setTrainId(String trainId){
        this.trainId = trainId;
    }

    public void setSource(String source){
        this.source = source;
    }

    public void setDestination(String destination){
        this.destination = destination;
    }

    public void setDateOfTravel(String dateOfTravel){
        this.dateOfTravel = dateOfTravel;
    }

    public void setSeatNo(String seatNo){
        this.seatNo = seatNo;
    }

    public String getTicketId(){
        return this.ticketId;
    }

    public String getPnr(){
        return this.pnr;
    }

    public String getUserId(){
        return this.userId;
    }

    public String getTrainId(){
        return this.trainId;
    }

    public String getSource(){
        return this.source;
    }

    public String getDestination(){
        return this.destination;
    }

    public String getDateOfTravel(){
        return this.dateOfTravel;
    }

    public String getSeatNo(){
        return this.seatNo;
    }
}