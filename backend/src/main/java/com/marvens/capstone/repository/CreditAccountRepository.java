package com.marvens.capstone.repository;

import java.util.Optional;

import com.marvens.capstone.entity.CreditAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {
    Optional<CreditAccount> findByUser_Id(Long userId);

    Optional<CreditAccount> findByIdAndUser_Id(Long accountId, Long userId);

    Page<CreditAccount> findAllByOrderByIdAsc(Pageable pageable);
}
