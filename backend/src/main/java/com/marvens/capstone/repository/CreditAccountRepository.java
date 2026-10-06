package com.marvens.capstone.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;

import com.marvens.capstone.entity.CreditAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {
    Optional<CreditAccount> findByUser_Id(Long userId);

    Optional<CreditAccount> findByIdAndUser_Id(Long accountId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CreditAccount a where a.id = :accountId and a.user.id = :userId")
    Optional<CreditAccount> findOwnedForUpdate(@Param("accountId") Long accountId,
                                             @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "user")
    @Query("select a from CreditAccount a where a.id = :accountId")
    Optional<CreditAccount> findForUpdate(@Param("accountId") Long accountId);

    @EntityGraph(attributePaths = "user")
    Page<CreditAccount> findAllByOrderByIdAsc(Pageable pageable);
}
