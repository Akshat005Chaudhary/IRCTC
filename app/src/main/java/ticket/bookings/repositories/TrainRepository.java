package ticket.bookings.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ticket.bookings.entities.Train;
import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface TrainRepository extends JpaRepository<Train, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Train t WHERE t.trainId = :id")
    Optional<Train> findByIdForUpdate(@Param("id") String id);
}
