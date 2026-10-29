package com.nfpl.access.repo;
import com.nfpl.access.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface AuditEventRepository extends JpaRepository<AuditEvent,Long> {
 Page<AuditEvent> findByJobIdOrderByOccurredAtDesc(Long jobId, Pageable pageable);
 Page<AuditEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
