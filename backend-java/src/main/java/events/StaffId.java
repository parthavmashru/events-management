package events;

import jakarta.persistence.*;

@Entity
@Table(name = "staff_ids")
public class StaffId {

    @Id
    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private User.Role role;

    @Column(nullable = false)
    private boolean used = false;

    public StaffId() {}

    public StaffId(String code, User.Role role, boolean used) {
        this.code = code;
        this.role = role;
        this.used = used;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public User.Role getRole() { return role; }
    public void setRole(User.Role role) { this.role = role; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
