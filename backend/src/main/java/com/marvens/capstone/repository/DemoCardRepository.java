package com.marvens.capstone.repository;

import com.marvens.capstone.entity.DemoCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface DemoCardRepository extends JpaRepository<DemoCard, Long> {
    // The entry hint reads the assigned account ID after the service returns.
    @EntityGraph(attributePaths = "account")
    DemoCard findByAccount_Id(Long accountId);
    DemoCard findByIdAndAccount_Id(Long cardId, Long accountId);
}
