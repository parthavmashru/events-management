package events;

import events.Dtos.ParticipationReportDto;
import events.Dtos.RegistrationResponse;
import events.Dtos.RosterEntryDto;
import events.GlobalExceptionHandler.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public RegistrationService(RegistrationRepository registrationRepository,
                               EventRepository eventRepository,
                               UserRepository userRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RegistrationResponse registerForEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        boolean alreadyRegistered = registrationRepository
                .findFirstByEventIdAndUserIdAndStatus(eventId, userId, Registration.Status.REGISTERED)
                .isPresent();
        if (alreadyRegistered) {
            throw new ApiException(HttpStatus.CONFLICT, "Already registered for this event");
        }

        long activeCount = registrationRepository.countByEventIdAndStatus(eventId, Registration.Status.REGISTERED);
        if (activeCount >= event.getCapacity()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is at full capacity");
        }

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setUser(user);
        registration.setStatus(Registration.Status.REGISTERED);

        return RegistrationResponse.from(registrationRepository.save(registration), false);
    }

    @Transactional
    public void cancelRegistration(Long eventId, Long userId) {
        Registration registration = registrationRepository
                .findFirstByEventIdAndUserIdAndStatus(eventId, userId, Registration.Status.REGISTERED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Registration not found"));

        registration.setStatus(Registration.Status.CANCELLED);
        registrationRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getMyRegistrations(Long userId) {
        return registrationRepository.findByUserId(userId).stream()
                .map(reg -> RegistrationResponse.from(reg, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipationReportDto> getParticipationReport() {
        return eventRepository.findAllByOrderByDateAsc().stream().map(event -> {
            long registered = registrationRepository.countByEventIdAndStatus(event.getId(), Registration.Status.REGISTERED);
            long attended = registrationRepository.countByEventIdAndStatus(event.getId(), Registration.Status.ATTENDED);
            int capacity = event.getCapacity() != null ? event.getCapacity() : 0;
            long fillRate = capacity > 0 ? Math.round((registered * 100.0) / capacity) : 0;
            return new ParticipationReportDto(
                    event.getId(), event.getTitle(), event.getDate(),
                    event.getCapacity(), registered, attended, fillRate
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<RosterEntryDto> getEventRoster(Long eventId) {
        return registrationRepository.findByEventId(eventId).stream()
                .map(RosterEntryDto::from)
                .toList();
    }
}
