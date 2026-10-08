package com.marvens.capstone.repository;

import java.util.List;
import com.marvens.capstone.entity.CreditAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {
    CreditAccount findByUser_Id(Long userId);
    CreditAccount findByIdAndUser_Id(Long accountId, Long userId);
    List<CreditAccount> findAllByOrderByIdAsc();

    // A balance-changing request waits until the previous one releases this account.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CreditAccount a where a.id = :accountId and a.user.id = :userId")
    CreditAccount findOwnedForUpdate(@Param("accountId") Long accountId, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CreditAccount a where a.id = :accountId")
    CreditAccount findForUpdate(@Param("accountId") Long accountId);
}
