package ticket.bookings.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ticket.bookings.entities.Train;

@Repository
public interface TrainRepository extends JpaRepository<Train, String> {
}
