package events;

import events.Dtos.EventRequest;
import events.Dtos.EventResponse;
import events.GlobalExceptionHandler.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;

    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAllByOrderByDateAsc().stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findWithOrganizerById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        return EventResponse.from(event);
    }

    @Transactional
    public EventResponse createEvent(EventRequest request, Long creatorUserId) {
        User organizer = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Organizer account not found"));

        Event event = new Event();
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setDate(parseDateTime(request.date()));
        event.setCapacity(request.capacity() != null ? request.capacity() : 100);
        event.setOrganizer(organizer);

        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = eventRepository.findWithOrganizerById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setDate(parseDateTime(request.date()));
        if (request.capacity() != null) {
            event.setCapacity(request.capacity());
        }

        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        registrationRepository.deleteByEventId(id);
        eventRepository.delete(event);
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "title and date are required");
        }
        String trimmed = raw.trim();
        try { return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME); } catch (DateTimeParseException ignored) {}
        try { return OffsetDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime(); } catch (DateTimeParseException ignored) {}
        try { return LocalDateTime.ofInstant(Instant.parse(trimmed), ZoneOffset.UTC); } catch (DateTimeParseException ignored) {}
        try { return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(); } catch (DateTimeParseException ignored) {}
        throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid date format");
    }
}
