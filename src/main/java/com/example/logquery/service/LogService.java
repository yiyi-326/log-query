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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LogService {

    private final LogEntryRepository repository;

    public LogService(LogEntryRepository repository) {
        this.repository = repository;
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
    public List<LogEntry> saveBatch(List<LogEntry> entries) {
        return repository.saveAll(entries);
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

            // 正则表达式搜索
            if (req.getRegex() != null && !req.getRegex().isBlank()) {
                String pattern = req.getRegex().trim();
                predicates.add(cb.or(
                        cb.isTrue(cb.function("REGEXP_LIKE", Boolean.class,
                                root.get("message"), cb.literal(pattern))),
                        cb.isTrue(cb.function("REGEXP_LIKE", Boolean.class,
                                root.get("source"), cb.literal(pattern)))
                ));
            }

            // 多关键词组合搜索
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
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LogStatsResponse stats = new LogStatsResponse();

        stats.setTotalCount(repository.count());
        stats.setTodayCount(repository.countByTimestampAfter(todayStart));

        Map<String, Long> levelDist = new LinkedHashMap<>();
        levelDist.put("DEBUG", 0L);
        levelDist.put("INFO", 0L);
        levelDist.put("WARN", 0L);
        levelDist.put("ERROR", 0L);
        for (Object[] row : repository.countByLevel()) {
            levelDist.put((String) row[0], (Long) row[1]);
        }
        stats.setLevelDistribution(levelDist);

        List<Object[]> sourceRows = repository.countBySource();
        stats.setTopSources(sourceRows.stream()
                .limit(10)
                .map(row -> new LogStatsResponse.SourceCount((String) row[0], (Long) row[1]))
                .collect(Collectors.toList()));

        Map<String, Long> hourlyMap = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            hourlyMap.put(String.format("%02d:00", h), 0L);
        }
        List<LogEntry> todayLogs = repository.findByTimestampAfter(todayStart);
        for (LogEntry e : todayLogs) {
            String hourKey = String.format("%02d:00", e.getTimestamp().getHour());
            hourlyMap.merge(hourKey, 1L, Long::sum);
        }
        stats.setHourlyTrend(hourlyMap.entrySet().stream()
                .map(e -> new LogStatsResponse.HourlyCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));

        return stats;
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
