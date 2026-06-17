package com.example.logquery.service;

import com.example.logquery.entity.LogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogImportService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern COMMON_LOG_PATTERN = Pattern.compile(
            "^(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}[T ]\\d{1,2}:\\d{2}:\\d{2}(?:\\.\\d{3})?)\\s*" +
                    "\\[?(DEBUG|INFO|WARN(?:ING)?|ERROR|TRACE)\\]?\\s*" +
                    "(\\S+)?\\s*[-:]?\\s*" +
                    "(.*)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    public List<LogEntry> parse(MultipartFile file) throws Exception {
        List<LogEntry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                LogEntry entry = parseJsonLine(line);
                if (entry == null) {
                    entry = parseCommonLogLine(line);
                }
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    private LogEntry parseJsonLine(String line) {
        try {
            if (line.startsWith("{")) {
                LogEntry entry = objectMapper.readValue(line, LogEntry.class);
                if (entry.getLevel() != null) {
                    entry.setLevel(entry.getLevel().toUpperCase());
                }
                return entry;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private LogEntry parseCommonLogLine(String line) {
        Matcher m = COMMON_LOG_PATTERN.matcher(line);
        if (m.matches()) {
            String timeStr = m.group(1);
            String level = m.group(2).toUpperCase();
            String source = m.group(3);
            String message = m.group(4);

            if ("WARNING".equals(level)) level = "WARN";

            LocalDateTime timestamp = parseDateTime(timeStr);
            return new LogEntry(timestamp, level, source != null ? source : "unknown", message);
        }
        return null;
    }

    private LocalDateTime parseDateTime(String str) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDateTime.parse(str, fmt);
            } catch (Exception ignored) {
            }
        }
        return LocalDateTime.now();
    }
}
