package com.marvens.capstone.repository;

import java.util.Optional;

import com.marvens.capstone.entity.DemoCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoCardRepository extends JpaRepository<DemoCard, Long> {
    Optional<DemoCard> findByAccount_Id(Long accountId);

    Optional<DemoCard> findByIdAndAccount_Id(Long cardId, Long accountId);
}
