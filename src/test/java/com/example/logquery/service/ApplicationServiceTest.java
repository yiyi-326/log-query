package com.example.logquery.service;

import com.example.logquery.entity.Application;
import com.example.logquery.repository.ApplicationRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository repository;

    private ApplicationService initService() {
        return new ApplicationService(repository);
    }

    @Nested
    class Create {

        @Test
        void shouldCreateAppWithGeneratedApiKey() {
            when(repository.existsByName("order-service")).thenReturn(false);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0, Application.class));

            Application app = new Application("order-service", "订单服务");
            Application created = initService().create(app);

            assertNotNull(created.getApiKey());
            assertTrue(created.getApiKey().startsWith("ak-"));
            assertTrue(created.getApiKey().length() > 32);
            assertTrue(created.isEnabled());
        }

        @Test
        void shouldRejectDuplicateName() {
            when(repository.existsByName("order-service")).thenReturn(true);
            Application app = new Application("order-service", "订单服务");
            ApplicationService service = initService();

            assertThrows(IllegalArgumentException.class, () -> service.create(app));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    class Update {

        @Test
        void shouldUpdateExistingApp() {
            Application existing = new Application("old-name", "old desc");
            existing.setId(1L);
            existing.setApiKey("ak-oldkey");

            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.existsByName("new-name")).thenReturn(false);
            when(repository.save(any())).thenReturn(existing);

            Application update = new Application("new-name", "new desc");
            update.setEnabled(false);
            Application result = initService().update(1L, update);

            assertEquals("new-name", result.getName());
            assertEquals("new desc", result.getDescription());
            assertFalse(result.isEnabled());
            assertEquals("ak-oldkey", result.getApiKey()); // key unchanged
        }

        @Test
        void shouldAllowSameNameUpdate() {
            Application existing = new Application("my-app", "desc");
            existing.setId(1L);

            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.save(any())).thenReturn(existing);

            Application update = new Application("my-app", "updated desc");
            update.setEnabled(true);
            initService().update(1L, update);

            verify(repository).save(existing);
        }

        @Test
        void shouldRejectUpdateToDuplicateName() {
            when(repository.findById(1L)).thenReturn(Optional.of(new Application("app-a", "")));
            when(repository.existsByName("app-b")).thenReturn(true);
            Application update = new Application("app-b", "");
            ApplicationService service = initService();

            assertThrows(IllegalArgumentException.class, () -> service.update(1L, update));
        }

        @Test
        void shouldThrowWhenAppNotFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());
            ApplicationService service = initService();
            Application empty = new Application();

            assertThrows(IllegalArgumentException.class, () -> service.update(999L, empty));
        }
    }

    @Nested
    class RegenerateKey {

        @Test
        void shouldGenerateNewKey() {
            Application app = new Application("my-app", "");
            app.setId(1L);
            app.setApiKey("ak-oldkey");

            when(repository.findById(1L)).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            Application result = initService().regenerateApiKey(1L);

            assertNotNull(result.getApiKey());
            assertTrue(result.getApiKey().startsWith("ak-"));
            assertNotEquals("ak-oldkey", result.getApiKey());
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteExistingApp() {
            when(repository.existsById(1L)).thenReturn(true);

            initService().delete(1L);

            verify(repository).deleteById(1L);
        }

        @Test
        void shouldThrowWhenDeletingNonExistent() {
            when(repository.existsById(999L)).thenReturn(false);
            ApplicationService service = initService();

            assertThrows(IllegalArgumentException.class, () -> service.delete(999L));
        }
    }

    @Nested
    class Query {

        @Test
        void shouldListAll() {
            when(repository.findAll()).thenReturn(List.of(
                    new Application("a", ""), new Application("b", "")
            ));

            List<Application> result = initService().listAll();

            assertEquals(2, result.size());
        }

        @Test
        void shouldListEnabled() {
            when(repository.findByEnabledTrue()).thenReturn(List.of(
                    new Application("a", "")
            ));

            List<Application> result = initService().listEnabled();

            assertEquals(1, result.size());
        }

        @Test
        void shouldGetById() {
            Application app = new Application("test", "");
            when(repository.findById(1L)).thenReturn(Optional.of(app));

            Optional<Application> result = initService().getById(1L);

            assertTrue(result.isPresent());
        }

        @Test
        void shouldGetByApiKey() {
            Application app = new Application("test", "");
            app.setApiKey("ak-key123");
            when(repository.findByApiKey("ak-key123")).thenReturn(Optional.of(app));

            Optional<Application> result = initService().getByApiKey("ak-key123");

            assertTrue(result.isPresent());
            assertEquals("test", result.get().getName());
        }
    }
}
