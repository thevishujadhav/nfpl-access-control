package com.nfpl.access.domain;

import jakarta.persistence.*;

@Entity
@Table(name="staff_users")
public class StaffUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true) private String username;
    @Column(nullable=false) private String passwordHash;
    @Column(nullable=false) private String displayName;
    @Column(nullable=false) private boolean active = true;
    @Column(nullable=false) private boolean absent = false;
    @Column(nullable=false) private String accountType = "STAFF";
    public Long getId(){return id;} public String getUsername(){return username;}
    public String getPasswordHash(){return passwordHash;} public String getDisplayName(){return displayName;}
    public boolean isActive(){return active;} public boolean isAbsent(){return absent;}
    public String getAccountType(){return accountType;}
    public void setUsername(String v){username=v;} public void setPasswordHash(String v){passwordHash=v;}
    public void setDisplayName(String v){displayName=v;} public void setActive(boolean v){active=v;}
    public void setAbsent(boolean v){absent=v;} public void setAccountType(String v){accountType=v;}
}
