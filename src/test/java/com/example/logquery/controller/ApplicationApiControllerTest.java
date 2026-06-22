package com.example.logquery.controller;

import com.example.logquery.entity.Application;
import com.example.logquery.service.ApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApplicationApiController.class)
class ApplicationApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationService appService;

    @Nested
    class ListApps {

        @Test
        void shouldListAllApps() throws Exception {
            Application app = new Application("order-service", "订单服务");
            app.setId(1L);
            when(appService.listAll()).thenReturn(List.of(app));

            mvc.perform(get("/api/v1/apps"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].name").value("order-service"));
        }

        @Test
        void shouldReturnEmptyList() throws Exception {
            when(appService.listAll()).thenReturn(List.of());

            mvc.perform(get("/api/v1/apps"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    class GetApp {

        @Test
        void shouldReturnAppById() throws Exception {
            Application app = new Application("my-app", "desc");
            app.setId(1L);
            when(appService.getById(1L)).thenReturn(Optional.of(app));

            mvc.perform(get("/api/v1/apps/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("my-app"));
        }

        @Test
        void shouldReturn404WhenNotFound() throws Exception {
            when(appService.getById(999L)).thenReturn(Optional.empty());

            mvc.perform(get("/api/v1/apps/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    class CreateApp {

        @Test
        void shouldCreateApp() throws Exception {
            Application input = new Application("new-app", "description");
            Application created = new Application("new-app", "description");
            created.setId(1L);
            created.setApiKey("ak-testkey123");

            when(appService.create(any())).thenReturn(created);

            mvc.perform(post("/api/v1/apps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(input)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.apiKey").value("ak-testkey123"));
        }

        @Test
        void shouldRejectEmptyName() throws Exception {
            when(appService.create(any())).thenThrow(new IllegalArgumentException("应用名称不能为空"));

            mvc.perform(post("/api/v1/apps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    class UpdateApp {

        @Test
        void shouldUpdateApp() throws Exception {
            Application update = new Application("updated", "new desc");
            when(appService.update(eq(1L), any())).thenReturn(update);

            mvc.perform(put("/api/v1/apps/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    class RegenerateKey {

        @Test
        void shouldRegenerateApiKey() throws Exception {
            Application app = new Application("app", "");
            app.setId(1L);
            app.setApiKey("ak-newkey456");
            when(appService.regenerateApiKey(1L)).thenReturn(app);

            mvc.perform(put("/api/v1/apps/1/regenerate-key"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.apiKey").value("ak-newkey456"));
        }
    }

    @Nested
    class DeleteApp {

        @Test
        void shouldDeleteApp() throws Exception {
            doNothing().when(appService).delete(1L);

            mvc.perform(delete("/api/v1/apps/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
