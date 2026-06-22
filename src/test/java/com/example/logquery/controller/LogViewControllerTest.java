package com.example.logquery.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogViewController.class)
class LogViewControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void shouldReturnIndexPage() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @ParameterizedTest
    @CsvSource({
            "/query, query",
            "/import, import",
            "/apps,  apps",
            "/alerts, alert-rules",
            "/alert-history, alert-history"
    })
    void shouldReturnCorrectView(String url, String viewName) throws Exception {
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(view().name(viewName));
    }
}
