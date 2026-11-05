package com.homeease.homeease.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "workers")
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "email_ver")
    private Integer emailVer = 0;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 15)
    private String mobile;

    private LocalDate dob;
    private String profileImage;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false, length = 6)
    private String pincode;

    @Column(nullable = false)
    private String category;

    @Column(unique = true, nullable = false)
    private String publicUserId;

    private int experience;

    private Double minCharge;
    private Double maxCharge;

    @Column(length = 1000)
    private String bio;

    @Column(nullable = false)
    private String idImagePath;

    private Double rating = 0.0;
    private Double latitude;
    private Double longitude;

    // 🆕 Online/Offline status
    @Column(nullable = false)
    private boolean online = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.WORKER;

    private boolean approved = false;
    private boolean enabled = false;

    private String verificationToken;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "declined_count", columnDefinition = "integer default 0")
    private int declinedCount = 0;

    // ✅ Bank Details Status
    private boolean bankDetailsSubmitted = false;
    private boolean bankDetailsVerified = false;

    // 🔊 Speaker Guide Setting
    @Column(nullable = false)
    private boolean speakerGuideEnabled = true;

    // ===== GETTERS & SETTERS =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getEmailVer() {
        return emailVer;
    }

    public void setEmailVer(int emailVer) {
        this.emailVer = emailVer;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public Double getMinCharge() {
        return minCharge;
    }

    public void setMinCharge(Double minCharge) {
        this.minCharge = minCharge;
    }

    public Double getMaxCharge() {
        return maxCharge;
    }

    public void setMaxCharge(Double maxCharge) {
        this.maxCharge = maxCharge;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getPublicUserId() {
        return publicUserId;
    }

    public void setPublicUserId(String publicUserId) {
        this.publicUserId = publicUserId;
    }

    public String getIdImagePath() {
        return idImagePath;
    }

    public void setIdImagePath(String idImagePath) {
        this.idImagePath = idImagePath;
    }

    // 🆕 Online getter/setter
    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public int getDeclinedCount() {
        return declinedCount;
    }

    public void setDeclinedCount(int declinedCount) {
        this.declinedCount = declinedCount;
    }

    public boolean isBankDetailsSubmitted() {
        return bankDetailsSubmitted;
    }

    public void setBankDetailsSubmitted(boolean bankDetailsSubmitted) {
        this.bankDetailsSubmitted = bankDetailsSubmitted;
    }

    public boolean isBankDetailsVerified() {
        return bankDetailsVerified;
    }

    public void setBankDetailsVerified(boolean bankDetailsVerified) {
        this.bankDetailsVerified = bankDetailsVerified;
    }

    public boolean isSpeakerGuideEnabled() {
        return speakerGuideEnabled;
    }

    public void setSpeakerGuideEnabled(boolean speakerGuideEnabled) {
        this.speakerGuideEnabled = speakerGuideEnabled;
    }

}