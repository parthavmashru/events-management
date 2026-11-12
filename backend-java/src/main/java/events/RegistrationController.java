package events;

import events.Dtos.*;
import events.SecurityConfig.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/registrations/{eventId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<RegistrationResponse> register(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.registerForEvent(eventId, principal.id()));
    }

    @DeleteMapping("/registrations/{eventId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MessageResponse> cancel(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        registrationService.cancelRegistration(eventId, principal.id());
        return ResponseEntity.ok(new MessageResponse("Registration cancelled"));
    }

    @GetMapping("/registrations/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<RegistrationResponse>> myRegistrations(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(registrationService.getMyRegistrations(principal.id()));
    }

    @GetMapping("/reports/participation")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<List<ParticipationReportDto>> getParticipationReport() {
        return ResponseEntity.ok(registrationService.getParticipationReport());
    }

    @GetMapping("/reports/event/{id}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<List<RosterEntryDto>> getEventRoster(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.getEventRoster(id));
    }
}
