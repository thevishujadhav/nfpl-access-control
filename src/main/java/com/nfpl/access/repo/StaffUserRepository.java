package com.nfpl.access.repo;
import com.nfpl.access.domain.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface StaffUserRepository extends JpaRepository<StaffUser,Long> {
 Optional<StaffUser> findByUsername(String username);
}
