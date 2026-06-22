package com.example.logquery.repository;

import com.example.logquery.entity.LogEntry;
import org.springframework.data.domain.Pageable;
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

    long countByTimestampAfterAndAppId(LocalDateTime after, Long appId);

    List<LogEntry> findByTimestampAfter(LocalDateTime after);

    List<LogEntry> findByTimestampAfterAndAppId(LocalDateTime after, Long appId);

    @Query("SELECT e.level, COUNT(e) FROM LogEntry e WHERE (:appId IS NULL OR e.appId = :appId) GROUP BY e.level")
    List<Object[]> countByLevel(@Param("appId") Long appId);

    @Query("SELECT e.level, COUNT(e) FROM LogEntry e WHERE e.timestamp >= :since AND (:appId IS NULL OR e.appId = :appId) GROUP BY e.level")
    List<Object[]> countByLevelSince(@Param("since") LocalDateTime since, @Param("appId") Long appId);

    @Query("SELECT e.source, COUNT(e) FROM LogEntry e WHERE (:appId IS NULL OR e.appId = :appId) GROUP BY e.source ORDER BY COUNT(e) DESC")
    List<Object[]> countBySource(@Param("appId") Long appId);

    void deleteByTimestampBefore(LocalDateTime before);

    List<LogEntry> findByLevelOrderByTimestampDesc(String level, Pageable pageable);

    @Query("SELECT e FROM LogEntry e WHERE e.level = :level AND (:appId IS NULL OR e.appId = :appId) ORDER BY e.timestamp DESC")
    List<LogEntry> findRecentErrors(@Param("level") String level, @Param("appId") Long appId, Pageable pageable);
}
