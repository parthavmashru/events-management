package events;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findFirstByEventIdAndUserIdAndStatus(Long eventId, Long userId, Registration.Status status);

    long countByEventIdAndStatus(Long eventId, Registration.Status status);

    @EntityGraph(attributePaths = {"event", "event.organizer"})
    List<Registration> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<Registration> findByEventId(Long eventId);

    void deleteByEventId(Long eventId);
}
