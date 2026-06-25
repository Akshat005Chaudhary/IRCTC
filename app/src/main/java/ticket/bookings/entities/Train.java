package ticket.bookings.entities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.*;

@Entity
@Table(name = "trains")
public class Train{
    @Id
    private String trainId;
    private String trainNo;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "train_stations",
        joinColumns = @JoinColumn(name = "train_id")
    )
    @OrderColumn(name = "station_order")
    @Column(name = "station_name")
    private List<String> stations;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "train_station_times",
        joinColumns = @JoinColumn(name = "train_id")
    )
    @MapKeyColumn(name = "station_name")
    @Column(name = "arrival_departure_time")
    private Map<String, String> stationTimes;

    @Convert(converter = SeatsConverter.class)
    @Column(columnDefinition = "TEXT")
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