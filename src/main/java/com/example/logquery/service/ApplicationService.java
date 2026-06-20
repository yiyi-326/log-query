package com.example.logquery.service;

import com.example.logquery.entity.Application;
import com.example.logquery.repository.ApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationService {

    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    public List<Application> listAll() {
        return repository.findAll();
    }

    public List<Application> listEnabled() {
        return repository.findByEnabledTrue();
    }

    public Optional<Application> getById(Long id) {
        return repository.findById(id);
    }

    public Optional<Application> getByApiKey(String apiKey) {
        return repository.findByApiKey(apiKey);
    }

    @Transactional
    public Application create(Application app) {
        if (repository.existsByName(app.getName())) {
            throw new IllegalArgumentException("应用名称已存在: " + app.getName());
        }
        app.setApiKey(generateApiKey());
        app.setEnabled(true);
        return repository.save(app);
    }

    @Transactional
    public Application update(Long id, Application update) {
        Application existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("应用不存在: " + id));

        if (!existing.getName().equals(update.getName())
                && repository.existsByName(update.getName())) {
            throw new IllegalArgumentException("应用名称已存在: " + update.getName());
        }

        existing.setName(update.getName());
        if (update.getDescription() != null) {
            existing.setDescription(update.getDescription());
        }
        existing.setEnabled(update.isEnabled());
        return repository.save(existing);
    }

    @Transactional
    public Application regenerateApiKey(Long id) {
        Application app = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("应用不存在: " + id));
        app.setApiKey(generateApiKey());
        return repository.save(app);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("应用不存在: " + id);
        }
        repository.deleteById(id);
    }

    private String generateApiKey() {
        return "ak-" + UUID.randomUUID().toString().replace("-", "");
    }
}
