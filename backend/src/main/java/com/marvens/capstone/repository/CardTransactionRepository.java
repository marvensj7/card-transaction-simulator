package com.marvens.capstone.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.marvens.capstone.entity.CardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {
    CardTransaction findByAccount_IdAndRequestId(Long accountId, String requestId);
    CardTransaction findByIdAndAccount_User_Id(Long transactionId, Long userId);
    CardTransaction findByOriginalPurchase_Id(Long purchaseId);
    Page<CardTransaction> findByAccount_IdOrderByIdDesc(Long accountId, Pageable pageable);
    Page<CardTransaction> findAllByOrderByIdDesc(Pageable pageable);

    // Find just the account ID first; load its current balance under the account lock.
    @Query("select t.account.id from CardTransaction t where t.id = :transactionId and t.account.user.id = :userId")
    Long findOwnedAccountId(@Param("transactionId") Long transactionId, @Param("userId") Long userId);
}
