package com.nfpl.access.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="jobs", indexes={
 @Index(name="idx_job_scope", columnList="clientCode,branchCode,direction,mode,stage"),
 @Index(name="idx_job_owner", columnList="ownerUsername,stage")
})
public class JobRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false) private String title;
    @Column(nullable=false) private String clientCode;
    @Column(nullable=false) private String branchCode;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Direction direction;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ShipmentMode mode;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private JobStage stage = JobStage.CONFIRMED;
    @Column(nullable=false) private String ownerUsername;
    @Column(nullable=false) private boolean archived = false;
    @Version private long version;
    @Column(nullable=false) private Instant createdAt = Instant.now();
    @Column(nullable=false) private Instant updatedAt = Instant.now();

    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public Long getId(){return id;} public String getTitle(){return title;}
    public String getClientCode(){return clientCode;} public String getBranchCode(){return branchCode;}
    public Direction getDirection(){return direction;} public ShipmentMode getMode(){return mode;}
    public JobStage getStage(){return stage;} public String getOwnerUsername(){return ownerUsername;}
    public boolean isArchived(){return archived;} public long getVersion(){return version;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public void setTitle(String v){title=v;} public void setClientCode(String v){clientCode=v;}
    public void setBranchCode(String v){branchCode=v;} public void setDirection(Direction v){direction=v;}
    public void setMode(ShipmentMode v){mode=v;} public void setStage(JobStage v){stage=v;}
    public void setOwnerUsername(String v){ownerUsername=v;} public void setArchived(boolean v){archived=v;}
}
