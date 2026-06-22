package com.example.logquery.service;

import com.example.logquery.dto.LogQueryRequest;
import com.example.logquery.dto.LogStatsResponse;
import com.example.logquery.entity.LogEntry;
import com.example.logquery.repository.LogEntryRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LogService {

    private final LogEntryRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public LogService(LogEntryRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public LogEntry save(LogEntry entry) {
        if (entry.getTimestamp() == null) {
            entry.setTimestamp(LocalDateTime.now());
        }
        if (entry.getLevel() == null) {
            entry.setLevel("INFO");
        }
        return repository.save(entry);
    }

    @Transactional
    public int[][] saveBatch(List<LogEntry> entries, Long appId) {
        if (entries.isEmpty()) return new int[0][0];
        LocalDateTime now = LocalDateTime.now();
        return jdbcTemplate.batchUpdate(
                "INSERT INTO log_entries (timestamp, level, source, message, app_id, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                entries, 500,
                (ps, entry) -> {
                    ps.setTimestamp(1, Timestamp.valueOf(
                            entry.getTimestamp() != null ? entry.getTimestamp() : now));
                    ps.setString(2, entry.getLevel() != null ? entry.getLevel() : "INFO");
                    ps.setString(3, entry.getSource());
                    ps.setString(4, entry.getMessage());
                    if (appId != null) {
                        ps.setLong(5, appId);
                    } else {
                        ps.setNull(5, java.sql.Types.BIGINT);
                    }
                    ps.setTimestamp(6, Timestamp.valueOf(now));
                });
    }

    public Page<LogEntry> query(LogQueryRequest request) {
        Specification<LogEntry> spec = buildSpecification(request);
        PageRequest pageRequest = PageRequest.of(
                request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );
        return repository.findAll(spec, pageRequest);
    }

    public List<LogEntry> queryAll(LogQueryRequest request) {
        Specification<LogEntry> spec = buildSpecification(request);
        return repository.findAll(spec, Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    private Specification<LogEntry> buildSpecification(LogQueryRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 应用隔离
            if (req.getAppId() != null) {
                predicates.add(cb.equal(root.get("appId"), req.getAppId()));
            }

            if (req.getLevel() != null && !req.getLevel().isBlank()) {
                predicates.add(cb.equal(root.get("level"), req.getLevel().trim().toUpperCase()));
            }
            if (req.getSource() != null && !req.getSource().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("source")), "%" + req.getSource().trim().toLowerCase() + "%"));
            }
            if (req.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), req.getStartTime()));
            }
            if (req.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), req.getEndTime()));
            }

            if (req.getRegex() != null && !req.getRegex().isBlank()) {
                String pattern = req.getRegex().trim();
                predicates.add(cb.or(
                        cb.isTrue(cb.function("REGEXP_LIKE", Boolean.class,
                                root.get("message"), cb.literal(pattern))),
                        cb.isTrue(cb.function("REGEXP_LIKE", Boolean.class,
                                root.get("source"), cb.literal(pattern)))
                ));
            }

            List<String> kwList = buildKeywordList(req);
            if (!kwList.isEmpty()) {
                String logic = "AND".equalsIgnoreCase(req.getKeywordLogic()) ? "AND" : "OR";
                List<Predicate> kwPredicates = new ArrayList<>();
                for (String kw : kwList) {
                    String pattern = "%" + kw.toLowerCase() + "%";
                    kwPredicates.add(cb.or(
                            cb.like(cb.lower(root.get("message")), pattern),
                            cb.like(cb.lower(root.get("source")), pattern)
                    ));
                }
                if ("AND".equals(logic)) {
                    predicates.add(cb.and(kwPredicates.toArray(new Predicate[0])));
                } else {
                    predicates.add(cb.or(kwPredicates.toArray(new Predicate[0])));
                }
            } else if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String kw = "%" + req.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("message")), kw),
                        cb.like(cb.lower(root.get("source")), kw)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<String> buildKeywordList(LogQueryRequest req) {
        List<String> result = new ArrayList<>();
        if (req.getKeywords() != null) {
            for (String k : req.getKeywords()) {
                if (k != null && !k.isBlank()) {
                    result.add(k.trim());
                }
            }
        }
        return result;
    }

    public LogStatsResponse getStats() {
        return getStats(null);
    }

    public LogStatsResponse getStats(Long appId) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        long totalCount;
        long todayCount;
        if (appId != null) {
            Specification<LogEntry> appSpec = (root, q, cb) -> cb.equal(root.get("appId"), appId);
            Specification<LogEntry> todaySpec = (root, q, cb) -> cb.and(
                    cb.equal(root.get("appId"), appId),
                    cb.greaterThanOrEqualTo(root.get("timestamp"), todayStart)
            );
            totalCount = repository.count(appSpec);
            todayCount = repository.count(todaySpec);
        } else {
            totalCount = repository.count();
            todayCount = repository.countByTimestampAfter(todayStart);
        }

        Map<String, Long> levelDist = new LinkedHashMap<>();
        levelDist.put("DEBUG", 0L);
        levelDist.put("INFO", 0L);
        levelDist.put("WARN", 0L);
        levelDist.put("ERROR", 0L);
        for (Object[] row : repository.countByLevel(appId)) {
            levelDist.put((String) row[0], (Long) row[1]);
        }

        List<LogStatsResponse.SourceCount> topSources = repository.countBySource(appId).stream()
                .limit(10)
                .map(row -> new LogStatsResponse.SourceCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());

        Map<String, Long> hourlyMap = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            hourlyMap.put(String.format("%02d:00", h), 0L);
        }
        List<LogEntry> todayLogs = appId != null
                ? repository.findByTimestampAfterAndAppId(todayStart, appId)
                : repository.findByTimestampAfter(todayStart);
        for (LogEntry e : todayLogs) {
            String hourKey = String.format("%02d:00", e.getTimestamp().getHour());
            hourlyMap.merge(hourKey, 1L, Long::sum);
        }
        List<LogStatsResponse.HourlyCount> hourlyTrend = hourlyMap.entrySet().stream()
                .map(e -> new LogStatsResponse.HourlyCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        long totalErrors = levelDist.getOrDefault("ERROR", 0L);
        double errorRate = totalCount > 0 ? (totalErrors * 100.0 / totalCount) : 0;

        long todayErrors = 0;
        List<Object[]> todayLevels = repository.countByLevelSince(todayStart, appId);
        for (Object[] row : todayLevels) {
            if ("ERROR".equals(row[0])) {
                todayErrors = (Long) row[1];
                break;
            }
        }
        double todayErrorRate = todayCount > 0 ? (todayErrors * 100.0 / todayCount) : 0;

        return new LogStatsResponse(totalCount, todayCount, levelDist,
                topSources, hourlyTrend, errorRate, todayErrorRate);
    }

    public List<LogEntry> getRecentErrors(int count) {
        return getRecentErrors(count, null);
    }

    public List<LogEntry> getRecentErrors(int count, Long appId) {
        PageRequest pageable = PageRequest.of(0, count);
        if (appId != null) {
            return repository.findRecentErrors("ERROR", appId, pageable);
        }
        return repository.findByLevelOrderByTimestampDesc("ERROR", pageable);
    }

    public String exportCsv(List<LogEntry> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("时间,级别,来源,内容\r\n");
        for (LogEntry e : logs) {
            sb.append(escapeCsv(e.getTimestamp() != null ? e.getTimestamp().toString() : "")).append(',');
            sb.append(escapeCsv(e.getLevel())).append(',');
            sb.append(escapeCsv(e.getSource())).append(',');
            sb.append(escapeCsv(e.getMessage())).append("\r\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Transactional
    public long deleteBefore(LocalDateTime before) {
        long count = repository.count();
        repository.deleteByTimestampBefore(before);
        return count - repository.count();
    }
}
