package com.example.logquery.repository;

import com.example.logquery.entity.AlertRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    Page<AlertRecord> findAllByOrderByTriggeredAtDesc(Pageable pageable);

    long countByTriggeredAtAfter(LocalDateTime after);

    boolean existsByRuleIdAndTriggeredAtAfter(Long ruleId, LocalDateTime after);
}
