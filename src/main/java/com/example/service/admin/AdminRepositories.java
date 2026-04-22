package com.example.service.admin;

import com.example.model.AdminTeam;
import com.example.model.AdminUser;
import com.example.model.Udf;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminRepositories {

    private AdminRepositories() {
    }

    @Component
    public static class AdminTeamRepository {
        private final Map<String, AdminTeam> teams = new ConcurrentHashMap<>();

        public AdminTeamRepository() {
            seedTeam("Team A", "platform-devops", "2026-01-10T09:00:00Z");
            seedTeam("Team B", "team-b-devops", "2026-01-11T09:00:00Z");
            seedTeam("Team C", "team-c-devops", "2026-01-12T09:00:00Z");
            seedTeam("Team D1", "team-d1-devops", "2026-01-13T09:00:00Z");
            seedTeam("Yarden", "yarden-devops", "2026-01-14T09:00:00Z");
        }

        public List<AdminTeam> findAll() {
            return teams.values().stream()
                .sorted(Comparator.comparing(AdminTeam::getTeamName, String.CASE_INSENSITIVE_ORDER))
                .map(this::copy)
                .toList();
        }

        public Optional<AdminTeam> findById(String id) {
            return Optional.ofNullable(teams.get(id)).map(this::copy);
        }

        public Optional<AdminTeam> findByTeamName(String teamName) {
            return teams.values().stream()
                .filter(team -> team.getTeamName().equalsIgnoreCase(teamName))
                .findFirst()
                .map(this::copy);
        }

        public boolean existsByTeamName(String teamName) {
            return teams.values().stream().anyMatch(team -> team.getTeamName().equalsIgnoreCase(teamName));
        }

        public AdminTeam save(AdminTeam team) {
            teams.put(team.getId(), copy(team));
            return copy(team);
        }

        public void delete(String id) {
            teams.remove(id);
        }

        private void seedTeam(String teamName, String devopsName, String timestamp) {
            String id = slugify(teamName);
            teams.put(id, new AdminTeam(id, teamName, devopsName, timestamp, timestamp));
        }

        private AdminTeam copy(AdminTeam team) {
            return new AdminTeam(team.getId(), team.getTeamName(), team.getDevopsName(), team.getCreatedAt(), team.getUpdatedAt());
        }
    }

    @Component
    public static class AdminUserRepository {
        private static final Logger logger = LoggerFactory.getLogger(AdminUserRepository.class);

        private final Map<String, AdminUser> users = new ConcurrentHashMap<>();
        private final Map<String, String> userTeamsById = new ConcurrentHashMap<>();

        public AdminUserRepository(
                @Value("${app.mock-mode:true}") boolean mockMode,
                ObjectMapper objectMapper) {
            if (mockMode) {
                seedUser("a", "Team A", "2026-01-15T10:00:00Z");
                seedUser("b", "Team B", "2026-01-16T10:00:00Z");
                seedUser("yarden", "Yarden", "2026-01-17T10:00:00Z");
            } else {
                loadFromJson(objectMapper);
            }
        }

        private void loadFromJson(ObjectMapper objectMapper) {
            try (InputStream is = new ClassPathResource("adminuser.json").getInputStream()) {
                List<AdminUser> loaded = objectMapper.readValue(is, new TypeReference<>() {});
                loaded.forEach(user -> users.put(user.getId(), copy(user)));
                logger.info("Loaded {} admin users from adminuser.json", loaded.size());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load adminuser.json", e);
            }
        }

        public List<AdminUser> findAll() {
            return users.values().stream()
                .sorted(Comparator.comparing(AdminUser::getUserId, String.CASE_INSENSITIVE_ORDER))
                .map(this::copy)
                .toList();
        }

        public Optional<AdminUser> findById(String id) {
            return Optional.ofNullable(users.get(id)).map(this::copy);
        }

        public Optional<AdminUser> findByUserId(String userId) {
            return users.values().stream()
                .filter(user -> user.getUserId().equalsIgnoreCase(userId))
                .findFirst()
                .map(this::copy);
        }

        public boolean existsByUserId(String userId) {
            return users.values().stream().anyMatch(user -> user.getUserId().equalsIgnoreCase(userId));
        }

        public List<AdminUser> findByTeamName(String teamName) {
            return users.entrySet().stream()
                .filter(entry -> {
                    String assignedTeamName = userTeamsById.get(entry.getKey());
                    return assignedTeamName != null && assignedTeamName.equalsIgnoreCase(teamName);
                })
                .map(Map.Entry::getValue)
                .map(this::copy)
                .toList();
        }

        public AdminUser save(AdminUser user) {
            users.put(user.getId(), copy(user));
            return copy(user);
        }

        public AdminUser save(AdminUser user, String teamName) {
            users.put(user.getId(), copy(user));
            userTeamsById.put(user.getId(), teamName);
            return copy(user);
        }

        public void delete(String id) {
            users.remove(id);
            userTeamsById.remove(id);
        }

        public void renameTeam(String oldTeamName, String newTeamName) {
            userTeamsById.entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(oldTeamName))
                .forEach(entry -> userTeamsById.put(entry.getKey(), newTeamName));
        }

        private void seedUser(String userId, String teamName, String timestamp) {
            users.put(userId, new AdminUser(userId, userId, timestamp));
            userTeamsById.put(userId, teamName);
        }

        private AdminUser copy(AdminUser user) {
            return new AdminUser(user.getId(), user.getUserId(), user.getCreatedAt());
        }
    }

    @Component
    public static class AdminUdfRepository {
        private final Map<String, Udf> udfs = new ConcurrentHashMap<>();

        public AdminUdfRepository() {
            seed(new Udf("udf-t-1", "data_cleaner", Udf.UdfType.transformer,
                "Cleans and normalizes data fields", true, true, "1.2.0",
                "/udfs/transformers/data_cleaner.jar", "data_team",
                LocalDateTime.of(2026, 4, 10, 10, 30),
                LocalDateTime.of(2026, 3, 18, 10, 30),
                LocalDateTime.of(2026, 4, 12, 10, 30)));
            seed(new Udf("udf-t-2", "email_normalizer", Udf.UdfType.transformer,
                "Normalizes email addresses to lowercase and validates format", true, true, "1.0.5",
                "/udfs/transformers/email_normalizer.jar", "data_team",
                LocalDateTime.of(2026, 4, 8, 14, 15),
                LocalDateTime.of(2026, 3, 20, 10, 30),
                LocalDateTime.of(2026, 4, 11, 14, 15)));
            seed(new Udf("udf-t-3", "phone_formatter", Udf.UdfType.transformer,
                "Formats phone numbers to standard international format", true, true, "2.1.0",
                "/udfs/transformers/phone_formatter.jar", "data_team",
                LocalDateTime.of(2026, 3, 25, 9, 0),
                LocalDateTime.of(2026, 3, 15, 10, 30),
                LocalDateTime.of(2026, 4, 5, 16, 45)));
            seed(new Udf("udf-t-4", "date_standardizer", Udf.UdfType.transformer,
                "Standardizes date formats to ISO 8601 format", true, false, "1.5.2",
                "/udfs/transformers/date_standardizer.jar", "analytics_team",
                null,
                LocalDateTime.of(2026, 4, 1, 8, 0),
                LocalDateTime.of(2026, 4, 15, 11, 20)));
            seed(new Udf("udf-t-5", "currency_converter", Udf.UdfType.transformer,
                "Converts monetary values using configured exchange rates", true, true, "3.0.1",
                "/udfs/transformers/currency_converter.jar", "finance_team",
                LocalDateTime.of(2026, 4, 5, 13, 30),
                LocalDateTime.of(2026, 2, 28, 10, 30),
                LocalDateTime.of(2026, 4, 14, 10, 30)));

            seed(new Udf("udf-f-1", "duplicate_filter", Udf.UdfType.filter,
                "Filters out duplicate records based on key fields", true, true, "2.1.3",
                "/udfs/filters/duplicate_filter.jar", "data_team",
                LocalDateTime.of(2026, 4, 11, 10, 30),
                LocalDateTime.of(2026, 3, 19, 10, 30),
                LocalDateTime.of(2026, 4, 13, 10, 30)));
            seed(new Udf("udf-f-2", "age_validator", Udf.UdfType.filter,
                "Filters records by supported age ranges", true, true, "1.3.0",
                "/udfs/filters/age_validator.jar", "compliance_team",
                LocalDateTime.of(2026, 4, 9, 15, 45),
                LocalDateTime.of(2026, 3, 22, 10, 30),
                LocalDateTime.of(2026, 4, 10, 15, 45)));
            seed(new Udf("udf-f-3", "null_value_filter", Udf.UdfType.filter,
                "Removes records containing null values in critical fields", true, true, "1.1.1",
                "/udfs/filters/null_value_filter.jar", "data_quality_team",
                LocalDateTime.of(2026, 4, 2, 11, 0),
                LocalDateTime.of(2026, 3, 10, 10, 30),
                LocalDateTime.of(2026, 4, 2, 11, 0)));
            seed(new Udf("udf-f-4", "range_filter", Udf.UdfType.filter,
                "Filters records where values fall outside configured ranges", true, false, "1.8.2",
                "/udfs/filters/range_filter.jar", "analytics_team",
                null,
                LocalDateTime.of(2026, 3, 28, 9, 30),
                LocalDateTime.of(2026, 4, 16, 14, 0)));
            seed(new Udf("udf-f-5", "regex_pattern_filter", Udf.UdfType.filter,
                "Filters records using configurable regex patterns", true, true, "2.0.0",
                "/udfs/filters/regex_pattern_filter.jar", "data_validation_team",
                LocalDateTime.of(2026, 4, 6, 12, 15),
                LocalDateTime.of(2026, 3, 5, 10, 30),
                LocalDateTime.of(2026, 4, 12, 12, 15)));
        }

        public List<Udf> findAll() {
            return udfs.values().stream()
                .sorted(Comparator.comparing(Udf::getId, String.CASE_INSENSITIVE_ORDER))
                .map(this::copy)
                .toList();
        }

        public Optional<Udf> findById(String id) {
            return Optional.ofNullable(udfs.get(id)).map(this::copy);
        }

        public Udf save(Udf udf) {
            udfs.put(udf.getId(), copy(udf));
            return copy(udf);
        }

        public void delete(String id) {
            udfs.remove(id);
        }

        private void seed(Udf udf) {
            udfs.put(udf.getId(), copy(udf));
        }

        private Udf copy(Udf udf) {
            return new Udf(
                udf.getId(),
                udf.getName(),
                udf.getType(),
                udf.getDescription(),
                udf.isActive(),
                udf.isApproved(),
                udf.getVersion(),
                udf.getFilePath(),
                udf.getTeam(),
                udf.getDateApproved(),
                udf.getCreatedAt(),
                udf.getUpdatedAt()
            );
        }
    }

    public static String slugify(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    public static String nowIso() {
        return Instant.now().toString();
    }
}
