package com.marvens.capstone.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.marvens.capstone.entity.CreditAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {
    // Load the owner for getOwnerName before the service transaction closes.
    @EntityGraph(attributePaths = "user")
    CreditAccount findByUser_Id(Long userId);
    @EntityGraph(attributePaths = "user")
    CreditAccount findByIdAndUser_Id(Long accountId, Long userId);
    @EntityGraph(attributePaths = "user")
    Page<CreditAccount> findAllByOrderByIdAsc(Pageable pageable);

    // A balance-changing request waits until the previous one releases this account.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "user")
    CreditAccount findLockedByIdAndUser_Id(Long accountId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "user")
    CreditAccount findLockedById(Long accountId);
}
