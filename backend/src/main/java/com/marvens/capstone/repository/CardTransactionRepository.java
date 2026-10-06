package com.marvens.capstone.repository;

import java.util.Optional;

import com.marvens.capstone.entity.CardTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {
    Optional<CardTransaction> findByAccount_IdAndRequestId(Long accountId, String requestId);

    // The refund path supplies a transaction ID; ownership comes from its account.
    @Query("""
            select t from CardTransaction t
            where t.id = :transactionId and t.account.user.id = :userId
            """)
    Optional<CardTransaction> findOwnedById(@Param("transactionId") Long transactionId,
                                          @Param("userId") Long userId);

    // Read only the ID before locking, so the persistence context has no stale balance.
    @Query("select t.account.id from CardTransaction t where t.id = :transactionId and t.account.user.id = :userId")
    Optional<Long> findOwnedAccountId(@Param("transactionId") Long transactionId,
                                    @Param("userId") Long userId);

    Optional<CardTransaction> findByOriginalPurchase_Id(Long purchaseId);

    Page<CardTransaction> findByAccount_IdAndAccount_User_IdOrderByIdDesc(
            Long accountId, Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "account.user")
    Page<CardTransaction> findAllByOrderByIdDesc(Pageable pageable);
}
