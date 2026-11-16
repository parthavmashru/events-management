package events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CollegeEventApiIntegrationTest {

    private static Path tempDbFile;

    @DynamicPropertySource
    static void registerSqliteProps(DynamicPropertyRegistry registry) throws IOException {
        tempDbFile = Files.createTempFile("college-event-test-", ".sqlite");
        registry.add("app.database.path", () -> tempDbFile.toAbsolutePath().toString());
    }

    @AfterAll
    static void cleanup() {
        if (tempDbFile == null) {
            return;
        }
        // SQLite/Hikari keep a native file lock on Windows until the JVM exits.
        // Fail the suite on assertion errors, not on temp-file deletion.
        tempDbFile.toFile().deleteOnExit();
        Path wal = Path.of(tempDbFile.toString() + "-wal");
        Path shm = Path.of(tempDbFile.toString() + "-shm");
        wal.toFile().deleteOnExit();
        shm.toFile().deleteOnExit();
        tryDelete(tempDbFile);
        tryDelete(wal);
        tryDelete(shm);
    }

    private static void tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // left for deleteOnExit()
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static String adminToken;
    private static String studentToken;
    private static String facultyToken;
    private static long eventId;

    @Test
    @Order(1)
    void healthCheckReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @Order(2)
    void rejectsInvalidFacultyAndAdminIds() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad Faculty\",\"email\":\"badfac@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"WRONG99\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Faculty ID. Please enter a valid Faculty ID."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Missing Faculty\",\"email\":\"nofac@test.com\",\"password\":\"password123\",\"role\":\"faculty\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Faculty ID. Please enter a valid Faculty ID."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad Admin\",\"email\":\"badadm@test.com\",\"password\":\"password123\",\"role\":\"admin\",\"adminId\":\"FAKE01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Admin ID. Please enter a valid Admin ID."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Missing Admin\",\"email\":\"noadm@test.com\",\"password\":\"password123\",\"role\":\"admin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Admin ID. Please enter a valid Admin ID."));
    }

    @Test
    @Order(3)
    void registersAdminStudentAndFacultyWithValidIdsAndBlocksReuse() throws Exception {
        MvcResult adminRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin User\",\"email\":\"admin@test.com\",\"password\":\"password123\",\"role\":\"admin\",\"adminId\":\"ADM001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("admin"))
                .andReturn();
        adminToken = objectMapper.readTree(adminRes.getResponse().getContentAsString()).get("token").asText();

        // Already-used Admin ID must be rejected
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin Two\",\"email\":\"admin2@test.com\",\"password\":\"password123\",\"role\":\"admin\",\"adminId\":\"ADM001\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("This Admin ID has already been registered."));

        // Student registration requires no special ID
        MvcResult studentRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Student User\",\"email\":\"student@test.com\",\"password\":\"password123\",\"role\":\"student\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("student"))
                .andReturn();
        studentToken = objectMapper.readTree(studentRes.getResponse().getContentAsString()).get("token").asText();

        // Valid Faculty ID registration
        MvcResult facultyRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Faculty User\",\"email\":\"faculty@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"FAC101\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("faculty"))
                .andReturn();
        facultyToken = objectMapper.readTree(facultyRes.getResponse().getContentAsString()).get("token").asText();

        // Already-used Faculty ID must be rejected
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Faculty Two\",\"email\":\"faculty2@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"FAC101\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("This Faculty ID has already been registered."));
    }

    @Test
    @Order(4)
    void rejectsDuplicateEmailWithoutConsumingIdAndVerifiesLogin() throws Exception {
        // Duplicate email with unused FAC102 should fail 409 and NOT consume FAC102
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dup\",\"email\":\"admin@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"FAC102\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already registered"));

        // FAC102 should still be available
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Faculty Two\",\"email\":\"fac2@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"FAC102\"}"))
                .andExpect(status().isCreated());

        // Student login requires no special ID
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.com\",\"password\":\"password123\",\"role\":\"student\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("student"));

        // Faculty login requires valid registered Faculty ID assigned to the account (usable repeatedly)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"faculty@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"FAC101\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("faculty"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"faculty@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"fac101\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // Admin login requires valid registered Admin ID assigned to the account (usable repeatedly)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"password123\",\"role\":\"admin\",\"adminId\":\"ADM001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("admin"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"password123\",\"role\":\"admin\",\"adminId\":\"ADM001\"}"))
                .andExpect(status().isOk());

        // Wrong password
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"wrongpass\",\"role\":\"admin\",\"adminId\":\"ADM001\"}"))
                .andExpect(status().isUnauthorized());

        // Role mismatches must be rejected
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"FAC101\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"faculty@test.com\",\"password\":\"password123\",\"role\":\"student\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"password123\",\"role\":\"student\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"FAC101\"}"))
                .andExpect(status().isForbidden());

        // Faculty ID validation during login (invalid, unregistered, assigned to another account)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"faculty@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"WRONG99\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Faculty ID. Please enter a valid Faculty ID."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"faculty@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"FAC103\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This Faculty ID has not been registered yet."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"faculty@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"FAC102\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This Faculty ID is not assigned to your account."));

        // Admin ID validation during login (invalid, unregistered, assigned to another account)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"password123\",\"role\":\"admin\",\"adminId\":\"BAD000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Admin ID. Please enter a valid Admin ID."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"password123\",\"role\":\"admin\",\"adminId\":\"ADM002\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This Admin ID has not been registered yet."));

        // Register second admin with ADM002 and verify cross-account Admin ID is rejected
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin Two\",\"email\":\"admin2@test.com\",\"password\":\"password123\",\"role\":\"admin\",\"adminId\":\"ADM002\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"password123\",\"role\":\"admin\",\"adminId\":\"ADM002\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This Admin ID is not assigned to your account."));
    }

    @Test
    @Order(5)
    void enforcesRoleOnEventCreationAndListsEvents() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"date\":\"2026-08-01\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hack Night\",\"date\":\"2026-08-01\"}"))
                .andExpect(status().isForbidden());

        MvcResult created = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tech Fest\",\"description\":\"Annual tech fest\",\"location\":\"Auditorium\",\"date\":\"2026-09-15\",\"capacity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Tech Fest"))
                .andReturn();
        JsonNode node = objectMapper.readTree(created.getResponse().getContentAsString());
        eventId = node.get("id").asLong();

        mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Tech Fest"));
    }

    @Test
    @Order(6)
    void handlesRegistrationsCapacityAndCancellation() throws Exception {
        mockMvc.perform(post("/api/registrations/" + eventId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/registrations/" + eventId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/registrations/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].Event.title").value("Tech Fest"));

        MvcResult s2 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"S2\",\"email\":\"s2@test.com\",\"password\":\"password123\",\"role\":\"student\"}"))
                .andExpect(status().isCreated()).andReturn();
        String s2Token = objectMapper.readTree(s2.getResponse().getContentAsString()).get("token").asText();

        MvcResult s3 = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"S3\",\"email\":\"s3@test.com\",\"password\":\"password123\",\"role\":\"student\"}"))
                .andExpect(status().isCreated()).andReturn();
        String s3Token = objectMapper.readTree(s3.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(post("/api/registrations/" + eventId)
                        .header("Authorization", "Bearer " + s2Token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/registrations/" + eventId)
                        .header("Authorization", "Bearer " + s3Token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/registrations/" + eventId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration cancelled"));
    }

    @Test
    @Order(7)
    void enforcesRoleOnReportsAndEventUpdateDelete() throws Exception {
        mockMvc.perform(get("/api/reports/participation")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Tech Fest"));

        mockMvc.perform(get("/api/reports/event/" + eventId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].User.email").isNotEmpty());

        mockMvc.perform(get("/api/reports/participation")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tech Fest Updated\",\"description\":\"Updated\",\"location\":\"Main Hall\",\"date\":\"2026-09-15T10:30\",\"capacity\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tech Fest Updated"));

        mockMvc.perform(delete("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Event deleted"));
    }

    @Test
    @Order(8)
    void forgotAndResetPasswordEnforcesOwnershipExpirySingleUseAndHashesPassword() throws Exception {
        // Non-existent and existing email return identical non-enumerating 200 response
        MvcResult unknownRes = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@test.com\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult knownRes = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.com\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(unknownRes.getResponse().getContentAsString(), knownRes.getResponse().getContentAsString());
        assertFalse(knownRes.getResponse().getContentAsString().contains("resetToken"));

        // Verify token hash stored in DB is not plaintext and test expired + valid token reset
        User student = userRepository.findByEmail("student@test.com").orElseThrow();
        assertNotNull(student.getResetTokenHash());
        assertNotNull(student.getResetTokenExpiresAt());

        // Mismatched passwords rejected
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"anytoken\",\"newPassword\":\"newpass123\",\"confirmPassword\":\"otherpass123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Passwords do not match"));

        // Invalid token rejected
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"wrong-token-value\",\"newPassword\":\"newpass123\",\"confirmPassword\":\"newpass123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired reset token"));

        // Seed a known raw token hash to verify expiration and single-use behavior deterministically
        String knownToken = "secure-test-reset-token-123";
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(knownToken.getBytes(StandardCharsets.UTF_8));
        String knownHash = HexFormat.of().formatHex(digest);

        student.setResetTokenHash(knownHash);
        student.setResetTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        userRepository.save(student);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + knownToken + "\",\"newPassword\":\"newpass123\",\"confirmPassword\":\"newpass123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired reset token"));

        // Valid unexpired token resets password
        student.setResetTokenHash(knownHash);
        student.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
        userRepository.save(student);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + knownToken + "\",\"newPassword\":\"newpass123\",\"confirmPassword\":\"newpass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful. You can now log in with your new password."));

        // Token is single-use: reuse must fail
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + knownToken + "\",\"newPassword\":\"another123\",\"confirmPassword\":\"another123\"}"))
                .andExpect(status().isBadRequest());

        // Old password must fail; new password must succeed and not be stored plaintext
        User updated = userRepository.findByEmail("student@test.com").orElseThrow();
        assertNotEquals("newpass123", updated.getPassword());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.com\",\"password\":\"password123\",\"role\":\"student\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@test.com\",\"password\":\"newpass123\",\"role\":\"student\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @Order(9)
    void profileEndpointsPersistChangesAndPreventUnauthorizedOrCrossUserEdits() throws Exception {
        // Unauthenticated rejected
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hacker\"}"))
                .andExpect(status().isUnauthorized());

        // Fetch own profile and ensure password/reset fields are never exposed
        MvcResult profileRes = mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Faculty User"))
                .andExpect(jsonPath("$.role").value("faculty"))
                .andExpect(jsonPath("$.profilePicture").value(nullValue()))
                .andReturn();
        String profileJson = profileRes.getResponse().getContentAsString();
        assertFalse(profileJson.contains("password"));
        assertFalse(profileJson.contains("resetToken"));

        // Invalid picture format rejected
        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Parthav Mashru\",\"profilePicture\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());

        // Update name and picture; extra fields (role, staffId, id, email) must be ignored
        String tinyPngDataUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"email\":\"admin@test.com\",\"role\":\"admin\",\"staffId\":\"ADM001\",\"name\":\"Parthav Mashru\",\"profilePicture\":\"" + tinyPngDataUrl + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Parthav Mashru"))
                .andExpect(jsonPath("$.role").value("faculty"))
                .andExpect(jsonPath("$.email").value("faculty@test.com"))
                .andExpect(jsonPath("$.profilePicture").value(tinyPngDataUrl));

        // Verify Admin user was NOT touched and Faculty staffId remained FAC101
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        assertEquals("Admin User", admin.getName());
        assertEquals(User.Role.ADMIN, admin.getRole());

        User faculty = userRepository.findByEmail("faculty@test.com").orElseThrow();
        assertEquals("FAC101", faculty.getStaffId());
        assertEquals(User.Role.FACULTY, faculty.getRole());

        // Changes persist after fresh login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"faculty@test.com\",\"password\":\"password123\",\"role\":\"faculty\",\"facultyId\":\"FAC101\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Parthav Mashru"))
                .andExpect(jsonPath("$.user.profilePicture").value(tinyPngDataUrl));

        // Removing profile picture reverts to null
        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rahul Sharma\",\"profilePicture\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rahul Sharma"))
                .andExpect(jsonPath("$.profilePicture").value(nullValue()));
    }
}
