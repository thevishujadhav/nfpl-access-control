package com.nfpl.access.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="audit_events", indexes={
 @Index(name="idx_audit_job_time", columnList="jobId,occurredAt"),
 @Index(name="idx_audit_actor_time", columnList="actorUsername,occurredAt")
})
public class AuditEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String actorUsername;
    private Long jobId;
    @Column(nullable=false) private String action;
    @Column(nullable=false) private boolean allowed;
    @Column(nullable=false, length=1000) private String reason;
    @Column(nullable=false) private Instant occurredAt=Instant.now();
    public Long getId(){return id;} public String getActorUsername(){return actorUsername;}
    public Long getJobId(){return jobId;} public String getAction(){return action;}
    public boolean isAllowed(){return allowed;} public String getReason(){return reason;}
    public Instant getOccurredAt(){return occurredAt;}
    public void setActorUsername(String v){actorUsername=v;} public void setJobId(Long v){jobId=v;}
    public void setAction(String v){action=v;} public void setAllowed(boolean v){allowed=v;}
    public void setReason(String v){reason=v;}
}
