package com.example.logquery.dto;

import java.util.List;
import java.util.Map;

public record LogStatsResponse(
        long totalCount,
        long todayCount,
        Map<String, Long> levelDistribution,
        List<SourceCount> topSources,
        List<HourlyCount> hourlyTrend,
        double errorRate,
        double todayErrorRate) {

    public record SourceCount(String source, long count) {}

    public record HourlyCount(String hour, long count) {}
}
