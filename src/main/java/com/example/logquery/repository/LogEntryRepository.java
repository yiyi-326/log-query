package com.example.logquery.repository;

import com.example.logquery.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long>, JpaSpecificationExecutor<LogEntry> {

    long countByTimestampAfter(LocalDateTime after);

    List<LogEntry> findByTimestampAfter(LocalDateTime after);

    @Query("SELECT e.level, COUNT(e) FROM LogEntry e WHERE e.timestamp >= :since GROUP BY e.level")
    List<Object[]> countByLevelSince(@Param("since") LocalDateTime since);

    @Query("SELECT e.level, COUNT(e) FROM LogEntry e GROUP BY e.level")
    List<Object[]> countByLevel();

    @Query("SELECT e.source, COUNT(e) FROM LogEntry e GROUP BY e.source ORDER BY COUNT(e) DESC")
    List<Object[]> countBySource();

    @Query("SELECT e.source, COUNT(e) FROM LogEntry e WHERE e.timestamp >= :since GROUP BY e.source ORDER BY COUNT(e) DESC")
    List<Object[]> countBySourceSince(@Param("since") LocalDateTime since);

    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);

    void deleteByTimestampBefore(LocalDateTime before);

    List<LogEntry> findByLevelOrderByTimestampDesc(String level, org.springframework.data.domain.Pageable pageable);
}
