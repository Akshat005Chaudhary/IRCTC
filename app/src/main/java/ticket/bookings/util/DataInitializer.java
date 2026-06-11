package ticket.bookings.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ticket.bookings.entities.Train;
import ticket.bookings.repositories.TrainRepository;

import java.io.File;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final TrainRepository trainRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public DataInitializer(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (trainRepository.count() == 0) {
            System.out.println("H2 Database is empty. Seeding trains from trains.json...");

            // Support finding trains.json both in normal gradle run and subproject execution paths
            File trainsFile = new File("app/src/main/java/ticket/localDb/trains.json");
            if (!trainsFile.exists()) {
                trainsFile = new File("src/main/java/ticket/localDb/trains.json");
            }

            if (trainsFile.exists()) {
                List<Train> trains = objectMapper.readValue(trainsFile, new TypeReference<List<Train>>() {});
                trainRepository.saveAll(trains);
                System.out.println("Successfully seeded " + trains.size() + " trains into H2 database.");
            } else {
                System.out.println("Warning: trains.json not found at expected paths. Skipping database seeding.");
            }
        } else {
            System.out.println("H2 Database already contains train data. Skipping seeding.");
        }
    }
}
