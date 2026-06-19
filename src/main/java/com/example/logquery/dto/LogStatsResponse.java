package com.example.logquery.dto;

import java.util.List;
import java.util.Map;

public class LogStatsResponse {
    private long totalCount;
    private long todayCount;
    private Map<String, Long> levelDistribution;
    private List<SourceCount> topSources;
    private List<HourlyCount> hourlyTrend;
    private double errorRate;
    private double todayErrorRate;

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }

    public long getTodayCount() { return todayCount; }
    public void setTodayCount(long todayCount) { this.todayCount = todayCount; }

    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double errorRate) { this.errorRate = errorRate; }

    public double getTodayErrorRate() { return todayErrorRate; }
    public void setTodayErrorRate(double todayErrorRate) { this.todayErrorRate = todayErrorRate; }

    public Map<String, Long> getLevelDistribution() { return levelDistribution; }
    public void setLevelDistribution(Map<String, Long> levelDistribution) { this.levelDistribution = levelDistribution; }

    public List<SourceCount> getTopSources() { return topSources; }
    public void setTopSources(List<SourceCount> topSources) { this.topSources = topSources; }

    public List<HourlyCount> getHourlyTrend() { return hourlyTrend; }
    public void setHourlyTrend(List<HourlyCount> hourlyTrend) { this.hourlyTrend = hourlyTrend; }

    public static class SourceCount {
        private String source;
        private long count;
        public SourceCount(String source, long count) { this.source = source; this.count = count; }
        public String getSource() { return source; }
        public long getCount() { return count; }
    }

    public static class HourlyCount {
        private String hour;
        private long count;
        public HourlyCount(String hour, long count) { this.hour = hour; this.count = count; }
        public String getHour() { return hour; }
        public long getCount() { return count; }
    }
}

