package com.example.logquery.controller;

import com.example.logquery.entity.AlertRecord;
import com.example.logquery.entity.AlertRule;
import com.example.logquery.service.AlertService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertApiController.class)
class AlertApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertService alertService;

    @Nested
    class Rules {

        @Test
        void shouldListRules() throws Exception {
            when(alertService.listRules()).thenReturn(List.of());

            mvc.perform(get("/api/v1/alerts/rules"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        void shouldGetRuleById() throws Exception {
            AlertRule rule = new AlertRule();
            rule.setId(1L);
            rule.setName("test");
            when(alertService.getRule(1L)).thenReturn(rule);

            mvc.perform(get("/api/v1/alerts/rules/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("test"));
        }

        @Test
        void shouldCreateRule() throws Exception {
            AlertRule rule = new AlertRule();
            rule.setName("new-rule");
            rule.setLevel("ERROR");
            rule.setThreshold(10);
            rule.setWindowMinutes(5);
            rule.setEnabled(true);

            when(alertService.createRule(any())).thenReturn(rule);

            mvc.perform(post("/api/v1/alerts/rules")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rule)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(containsString("创建成功")));
        }

        @Test
        void shouldUpdateRule() throws Exception {
            AlertRule rule = new AlertRule();
            rule.setName("updated");

            when(alertService.updateRule(eq(1L), any())).thenReturn(rule);

            mvc.perform(put("/api/v1/alerts/rules/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rule)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("更新成功")));
        }

        @Test
        void shouldDeleteRule() throws Exception {
            doNothing().when(alertService).deleteRule(1L);

            mvc.perform(delete("/api/v1/alerts/rules/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("已删除")));
        }
    }

    @Nested
    class Records {

        @Test
        void shouldListRecordsPaginated() throws Exception {
            AlertRecord record = new AlertRecord();
            Page<AlertRecord> page = new PageImpl<>(List.of(record));

            when(alertService.listRecords(0, 20)).thenReturn(page);

            mvc.perform(get("/api/v1/alerts/records"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        void shouldCapSizeAt100() throws Exception {
            when(alertService.listRecords(0, 100)).thenReturn(new PageImpl<>(List.of()));

            mvc.perform(get("/api/v1/alerts/records").param("size", "200"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class Count {

        @Test
        void shouldCount24hAlerts() throws Exception {
            when(alertService.count24h()).thenReturn(3L);

            mvc.perform(get("/api/v1/alerts/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(3));
        }
    }

    @Nested
    class Evaluate {

        @Test
        void shouldTriggerManualEvaluation() throws Exception {
            doNothing().when(alertService).evaluateRules();

            mvc.perform(post("/api/v1/alerts/evaluate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("已执行")));
        }
    }
}
