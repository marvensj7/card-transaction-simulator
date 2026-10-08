package com.marvens.capstone.repository;

import com.marvens.capstone.entity.DemoCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoCardRepository extends JpaRepository<DemoCard, Long> {
    DemoCard findByAccount_Id(Long accountId);
    DemoCard findByIdAndAccount_Id(Long cardId, Long accountId);
}
