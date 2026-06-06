package ticket.bookings.entities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Train{
    private String trainId;
    private String trainNo;
    private List<String> stations;
    private Map<String, String> stationTimes;
    private List<List<Integer>> seats;

    public Train(){
        this.trainId = "";
        this.trainNo = "";
        this.stations = new ArrayList<>();
        this.stationTimes = new HashMap<>();
        this.seats = new ArrayList<>();
    }

    public Train(String trainId, String trainNo, List<String> stations, Map<String, String> stationTimes, List<List<Integer>> seats){
        this.trainId = trainId;
        this.trainNo = trainNo;
        this.stations = stations;
        this.stationTimes = stationTimes;
        this.seats = seats;
    }

    public void setTrainId(String trainId){
        this.trainId = trainId;
    }

    public void setTrainNo(String trainNo){
        this.trainNo = trainNo;
    }

    public void setStations(List<String> stations){
        this.stations = stations;
    }

    public void setStationTimes(Map<String, String> stationTimes){
        this.stationTimes = stationTimes;
    }

    public void setSeats(List<List<Integer>> seats){
        this.seats = seats;
    }

    public String getTrainId(){
        return this.trainId;
    }

    public String getTrainNo(){
        return this.trainNo;
    }

    public List<String> getStations(){
        return this.stations;
    }

    public Map<String, String> getStationTimes(){
        return this.stationTimes;
    }

    public List<List<Integer>> getSeats(){
        return this.seats;
    }
}