package com.nfpl.access.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="permission_grants", indexes={
 @Index(name="idx_grant_user", columnList="username,role"),
 @Index(name="idx_grant_scope", columnList="clientCode,branchCode,direction,mode")
})
public class PermissionGrant {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String username;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private PermissionRole role;
    // "*" means any value in that dimension.
    @Column(nullable=false) private String clientCode="*";
    @Column(nullable=false) private String branchCode="*";
    @Column(nullable=false) private String direction="*";
    @Column(nullable=false) private String mode="*";
    @Column(nullable=false) private boolean active=true;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    @Column(nullable=false) private String grantedBy;
    public Long getId(){return id;} public String getUsername(){return username;}
    public PermissionRole getRole(){return role;} public String getClientCode(){return clientCode;}
    public String getBranchCode(){return branchCode;} public String getDirection(){return direction;}
    public String getMode(){return mode;} public boolean isActive(){return active;}
    public Instant getCreatedAt(){return createdAt;} public String getGrantedBy(){return grantedBy;}
    public void setUsername(String v){username=v;} public void setRole(PermissionRole v){role=v;}
    public void setClientCode(String v){clientCode=v;} public void setBranchCode(String v){branchCode=v;}
    public void setDirection(String v){direction=v;} public void setMode(String v){mode=v;}
    public void setActive(boolean v){active=v;} public void setGrantedBy(String v){grantedBy=v;}
}
