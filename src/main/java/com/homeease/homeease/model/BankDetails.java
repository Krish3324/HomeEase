package com.homeease.homeease.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_details")
public class BankDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "worker_id", nullable = false, unique = true)
    private Worker worker;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 20)
    private String accountNumberConfirm;

    @Column(nullable = false, length = 11)
    private String ifscCode;

    @Column(nullable = false, length = 50)
    private String bankName;

    @Column(nullable = false, length = 50)
    private String branchName;

    @Column(nullable = false)
    private String accountType; // SAVINGS, CURRENT, etc.

    @Column(nullable = false, length = 12)
    private String panNumber;

    @Column(nullable = false, length = 12)
    private String aadharNumber;

    @Column(nullable = false)
    private String upiId;

    // Verification status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    // Path to uploaded bank documents
    private String bankStatementPath;
    private String passBookPath;

    private String rejectionReason;
    private String adminNotes;

    // Timestamps
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== ENUMS =====
    public enum VerificationStatus {
        PENDING,
        VERIFIED,
        REJECTED,
        NEEDS_RESUBMISSION
    }

    // ===== GETTERS & SETTERS =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Worker getWorker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountNumberConfirm() {
        return accountNumberConfirm;
    }

    public void setAccountNumberConfirm(String accountNumberConfirm) {
        this.accountNumberConfirm = accountNumberConfirm;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getAadharNumber() {
        return aadharNumber;
    }

    public void setAadharNumber(String aadharNumber) {
        this.aadharNumber = aadharNumber;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getBankStatementPath() {
        return bankStatementPath;
    }

    public void setBankStatementPath(String bankStatementPath) {
        this.bankStatementPath = bankStatementPath;
    }

    public String getPassBookPath() {
        return passBookPath;
    }

    public void setPassBookPath(String passBookPath) {
        this.passBookPath = passBookPath;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
