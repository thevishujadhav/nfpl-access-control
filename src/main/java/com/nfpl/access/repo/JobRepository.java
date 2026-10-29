package com.nfpl.access.repo;
import com.nfpl.access.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface JobRepository extends JpaRepository<JobRecord,Long> {
 Page<JobRecord> findByOwnerUsernameAndArchivedFalse(String username, Pageable pageable);
 Page<JobRecord> findByArchivedFalse(Pageable pageable);
}
