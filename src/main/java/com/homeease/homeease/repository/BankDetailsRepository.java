package com.homeease.homeease.repository;

import com.homeease.homeease.model.BankDetails;
import com.homeease.homeease.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankDetailsRepository extends JpaRepository<BankDetails, Long> {

    Optional<BankDetails> findByWorker(Worker worker);

    @Query("SELECT bd FROM BankDetails bd WHERE bd.verificationStatus = 'PENDING' ORDER BY bd.submittedAt ASC")
    List<BankDetails> findPendingForVerification();

    @Query("SELECT bd FROM BankDetails bd WHERE bd.verificationStatus = 'VERIFIED' ORDER BY bd.verifiedAt DESC")
    List<BankDetails> findVerifiedBankDetails();

    @Query("SELECT bd FROM BankDetails bd WHERE bd.verificationStatus = 'REJECTED' ORDER BY bd.updatedAt DESC")
    List<BankDetails> findRejectedBankDetails();

    @Query("SELECT bd FROM BankDetails bd WHERE bd.verificationStatus = 'NEEDS_RESUBMISSION' ORDER BY bd.updatedAt DESC")
    List<BankDetails> findNeedsResubmission();

    @Query("SELECT bd FROM BankDetails bd WHERE bd.worker.id = :workerId")
    Optional<BankDetails> findByWorkerId(@Param("workerId") Long workerId);
}
