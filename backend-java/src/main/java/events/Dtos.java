package events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class Dtos {

    private Dtos() {}

    public record RegisterRequest(
            @NotBlank(message = "name, email and password are required") String name,
            @NotBlank(message = "name, email and password are required") @Email(message = "Valid email is required") String email,
            @NotBlank(message = "name, email and password are required") @Size(min = 6, message = "Password must be at least 6 characters") String password,
            String role,
            String facultyId,
            String adminId
    ) {}

    public record LoginRequest(
            @NotBlank(message = "email and password are required") String email,
            @NotBlank(message = "email and password are required") String password,
            @NotBlank(message = "Login role is required") String role,
            String facultyId,
            String adminId
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email is required") @Email(message = "Valid email is required") String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Reset token is required") String token,
            @NotBlank(message = "New password is required") @Size(min = 6, message = "Password must be at least 6 characters") String newPassword,
            @NotBlank(message = "Confirm password is required") String confirmPassword
    ) {}

    public record UpdateProfileRequest(
            @NotBlank(message = "Full name is required") @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters") String name,
            String profilePicture
    ) {}

    public record UserDto(Long id, String name, String email, User.Role role, String profilePicture) {
        public static UserDto from(User user) {
            return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getProfilePicture());
        }
    }

    public record AuthResponse(String token, UserDto user) {}

    public record EventRequest(
            @NotBlank(message = "title and date are required") String title,
            String description,
            String location,
            @NotBlank(message = "title and date are required") String date,
            @Min(value = 1, message = "Capacity must be at least 1") Integer capacity
    ) {}

    public record OrganizerDto(Long id, String name, String email) {
        public static OrganizerDto from(User user) {
            return user == null ? null : new OrganizerDto(user.getId(), user.getName(), user.getEmail());
        }
    }

    public record EventResponse(
            Long id,
            String title,
            String description,
            String location,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime date,
            Integer capacity,
            Long createdBy,
            OrganizerDto organizer,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt
    ) {
        public static EventResponse from(Event event) {
            User org = event.getOrganizer();
            return new EventResponse(
                    event.getId(),
                    event.getTitle(),
                    event.getDescription(),
                    event.getLocation(),
                    event.getDate(),
                    event.getCapacity(),
                    org != null ? org.getId() : null,
                    OrganizerDto.from(org),
                    event.getCreatedAt(),
                    event.getUpdatedAt()
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RegistrationResponse(
            Long id,
            Long eventId,
            Long userId,
            Registration.Status status,
            @JsonProperty("Event") EventResponse event,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt
    ) {
        public static RegistrationResponse from(Registration reg, boolean includeEvent) {
            return new RegistrationResponse(
                    reg.getId(),
                    reg.getEvent() != null ? reg.getEvent().getId() : null,
                    reg.getUser() != null ? reg.getUser().getId() : null,
                    reg.getStatus(),
                    includeEvent && reg.getEvent() != null ? EventResponse.from(reg.getEvent()) : null,
                    reg.getCreatedAt(),
                    reg.getUpdatedAt()
            );
        }
    }

    public record ParticipationReportDto(
            Long eventId,
            String title,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime date,
            Integer capacity,
            long registeredCount,
            long attendedCount,
            long fillRate
    ) {}

    public record RosterEntryDto(
            Long id,
            Long eventId,
            Long userId,
            Registration.Status status,
            @JsonProperty("User") UserDto user,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime updatedAt
    ) {
        public static RosterEntryDto from(Registration reg) {
            return new RosterEntryDto(
                    reg.getId(),
                    reg.getEvent() != null ? reg.getEvent().getId() : null,
                    reg.getUser() != null ? reg.getUser().getId() : null,
                    reg.getStatus(),
                    reg.getUser() != null ? UserDto.from(reg.getUser()) : null,
                    reg.getCreatedAt(),
                    reg.getUpdatedAt()
            );
        }
    }

    public record MessageResponse(String message) {}

    public record ErrorResponse(String error) {}
}
