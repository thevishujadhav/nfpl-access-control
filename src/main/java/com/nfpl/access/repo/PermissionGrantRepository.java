package com.nfpl.access.repo;
import com.nfpl.access.domain.PermissionGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PermissionGrantRepository extends JpaRepository<PermissionGrant,Long> {
 List<PermissionGrant> findByUsernameAndActiveTrue(String username);
}
