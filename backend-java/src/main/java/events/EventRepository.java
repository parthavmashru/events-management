package events;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"organizer"})
    List<Event> findAllByOrderByDateAsc();

    @EntityGraph(attributePaths = {"organizer"})
    Optional<Event> findWithOrganizerById(Long id);
}
