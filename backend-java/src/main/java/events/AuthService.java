package events;

import events.Dtos.*;
import events.GlobalExceptionHandler.ApiException;
import events.SecurityConfig.JwtHelper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern IMAGE_DATA_URL_PATTERN =
            Pattern.compile("^data:image/(png|jpeg|jpg|webp|gif);base64,[A-Za-z0-9+/=]+$");
    private static final int MAX_PROFILE_PICTURE_LENGTH = 2_800_000;
    private static final long RESET_TOKEN_TTL_MINUTES = 15L;

    private final UserRepository userRepository;
    private final StaffIdRepository staffIdRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtHelper jwtHelper;
    private final DataSource dataSource;
    private final SecureRandom secureRandom = new SecureRandom();

    private final Set<String> validFacultyIds = new HashSet<>();
    private final Set<String> validAdminIds = new HashSet<>();

    public AuthService(UserRepository userRepository,
                       StaffIdRepository staffIdRepository,
                       PasswordEncoder passwordEncoder,
                       JwtHelper jwtHelper,
                       DataSource dataSource) {
        this.userRepository = userRepository;
        this.staffIdRepository = staffIdRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtHelper = jwtHelper;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initPredefinedIds() throws IOException, SQLException {
        ensureUsersColumns();
        loadIds("faculty-ids.txt", User.Role.FACULTY, validFacultyIds);
        loadIds("admin-ids.txt", User.Role.ADMIN, validAdminIds);
    }

    private void ensureUsersColumns() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            boolean tableExists = false;
            Set<String> existingColumns = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)")) {
                while (rs.next()) {
                    tableExists = true;
                    existingColumns.add(rs.getString("name").toLowerCase());
                }
            }
            if (tableExists) {
                if (!existingColumns.contains("staff_id")) {
                    stmt.execute("ALTER TABLE users ADD COLUMN staff_id VARCHAR(255)");
                }
                if (!existingColumns.contains("profile_picture")) {
                    stmt.execute("ALTER TABLE users ADD COLUMN profile_picture TEXT");
                }
                if (!existingColumns.contains("reset_token_hash")) {
                    stmt.execute("ALTER TABLE users ADD COLUMN reset_token_hash VARCHAR(255)");
                }
                if (!existingColumns.contains("reset_token_expires_at")) {
                    stmt.execute("ALTER TABLE users ADD COLUMN reset_token_expires_at TIMESTAMP");
                }
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_staff_id ON users(staff_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_reset_token_hash ON users(reset_token_hash)");
            }
        }
    }

    private void loadIds(String resourceName, User.Role role, Set<String> targetSet) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourceName);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String code = line.trim().toUpperCase();
                if (!code.isEmpty() && !code.startsWith("#")) {
                    targetSet.add(code);
                    if (!staffIdRepository.existsById(code)) {
                        staffIdRepository.save(new StaffId(code, role, false));
                    }
                }
            }
        }
    }

    @Transactional
    public synchronized AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        User.Role role = User.Role.fromValue(request.role());
        String validatedStaffId = validateAndConsumeStaffId(role, request);

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setStaffId(validatedStaffId);

        try {
            User saved = userRepository.save(user);
            String token = jwtHelper.generateToken(saved.getId(), saved.getRole(), saved.getEmail());
            return new AuthResponse(token, UserDto.from(saved));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "Account or ID already registered");
        }
    }

    private String validateAndConsumeStaffId(User.Role role, RegisterRequest request) {
        if (role == User.Role.FACULTY) {
            String code = request.facultyId() != null ? request.facultyId().trim().toUpperCase() : "";
            if (code.isEmpty() || !validFacultyIds.contains(code)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Faculty ID. Please enter a valid Faculty ID.");
            }
            int updated = staffIdRepository.markAsUsedIfUnused(code, User.Role.FACULTY);
            if (updated == 0) {
                throw new ApiException(HttpStatus.CONFLICT, "This Faculty ID has already been registered.");
            }
            return code;
        }

        if (role == User.Role.ADMIN) {
            String code = request.adminId() != null ? request.adminId().trim().toUpperCase() : "";
            if (code.isEmpty() || !validAdminIds.contains(code)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Admin ID. Please enter a valid Admin ID.");
            }
            int updated = staffIdRepository.markAsUsedIfUnused(code, User.Role.ADMIN);
            if (updated == 0) {
                throw new ApiException(HttpStatus.CONFLICT, "This Admin ID has already been registered.");
            }
            return code;
        }

        return null;
    }

    public AuthResponse login(LoginRequest request) {
        User.Role selectedRole = parseLoginRole(request.role());

        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (user.getRole() != selectedRole) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Role mismatch. This account is not registered as " + formatRoleLabel(selectedRole) + "."
            );
        }

        if (selectedRole == User.Role.FACULTY) {
            String code = request.facultyId() != null ? request.facultyId().trim().toUpperCase() : "";
            if (code.isEmpty() || !validFacultyIds.contains(code)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Faculty ID. Please enter a valid Faculty ID.");
            }
            StaffId staffId = staffIdRepository.findById(code)
                    .filter(s -> s.getRole() == User.Role.FACULTY && s.isUsed())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "This Faculty ID has not been registered yet."));
            if (user.getStaffId() == null || !staffId.getCode().equalsIgnoreCase(user.getStaffId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "This Faculty ID is not assigned to your account.");
            }
        } else if (selectedRole == User.Role.ADMIN) {
            String code = request.adminId() != null ? request.adminId().trim().toUpperCase() : "";
            if (code.isEmpty() || !validAdminIds.contains(code)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Admin ID. Please enter a valid Admin ID.");
            }
            StaffId staffId = staffIdRepository.findById(code)
                    .filter(s -> s.getRole() == User.Role.ADMIN && s.isUsed())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "This Admin ID has not been registered yet."));
            if (user.getStaffId() == null || !staffId.getCode().equalsIgnoreCase(user.getStaffId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "This Admin ID is not assigned to your account.");
            }
        }

        String token = jwtHelper.generateToken(user.getId(), user.getRole(), user.getEmail());
        return new AuthResponse(token, UserDto.from(user));
    }

    private User.Role parseLoginRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Login role is required");
        }
        String normalized = roleValue.trim().toLowerCase();
        return switch (normalized) {
            case "student" -> User.Role.STUDENT;
            case "faculty" -> User.Role.FACULTY;
            case "admin" -> User.Role.ADMIN;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid login role selected.");
        };
    }

    private String formatRoleLabel(User.Role role) {
        return switch (role) {
            case STUDENT -> "Student";
            case FACULTY -> "Faculty";
            case ADMIN -> "Admin";
        };
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(user -> {
            byte[] tokenBytes = new byte[32];
            secureRandom.nextBytes(tokenBytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

            user.setResetTokenHash(sha256Hex(rawToken));
            user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES));
            userRepository.save(user);

            log.info("[PASSWORD RESET] Email delivery is not configured. Single-use reset token for {} (expires in {} min): {}",
                    email, RESET_TOKEN_TTL_MINUTES, rawToken);
        });

        return new MessageResponse(
                "If an account with that email exists, a single-use reset token (valid for 15 minutes) has been generated. " +
                "Email delivery is not configured in this environment, so check the backend server logs for the reset token."
        );
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        String tokenHash = sha256Hex(request.token().trim());
        User user = userRepository.findByResetTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

        if (user.getResetTokenExpiresAt() == null || user.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            user.setResetTokenHash(null);
            user.setResetTokenExpiresAt(null);
            userRepository.save(user);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetTokenHash(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);

        return new MessageResponse("Password reset successful. You can now log in with your new password.");
    }

    public UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return UserDto.from(user);
    }

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String cleanedName = request.name().trim();
        if (cleanedName.length() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name must be at least 2 characters");
        }
        user.setName(cleanedName);

        if (request.profilePicture() != null) {
            String picture = request.profilePicture().trim();
            if (picture.isEmpty()) {
                user.setProfilePicture(null);
            } else {
                if (picture.length() > MAX_PROFILE_PICTURE_LENGTH || !IMAGE_DATA_URL_PATTERN.matcher(picture).matches()) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "Invalid profile picture format. Upload a PNG, JPG, GIF, or WEBP image under 2 MB."
                    );
                }
                user.setProfilePicture(picture);
            }
        }

        User saved = userRepository.save(user);
        return UserDto.from(saved);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
