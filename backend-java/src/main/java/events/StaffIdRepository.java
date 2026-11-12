package events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffIdRepository extends JpaRepository<StaffId, String> {

    @Modifying
    @Query("UPDATE StaffId s SET s.used = true WHERE s.code = :code AND s.role = :role AND s.used = false")
    int markAsUsedIfUnused(@Param("code") String code, @Param("role") User.Role role);
}
