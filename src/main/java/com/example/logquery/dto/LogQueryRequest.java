package com.example.logquery.dto;

import java.time.LocalDateTime;
import java.util.List;

public class LogQueryRequest {
    private String keyword;
    private List<String> keywords;
    private String keywordLogic = "OR";
    private String regex;
    private String level;
    private String source;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int page = 0;
    private int size = 20;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public String getKeywordLogic() { return keywordLogic; }
    public void setKeywordLogic(String keywordLogic) { this.keywordLogic = keywordLogic; }

    public String getRegex() { return regex; }
    public void setRegex(String regex) { this.regex = regex; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
