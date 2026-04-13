package com.example.service.admin;

import com.example.model.AdminTeam;
import com.example.model.AdminUser;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
        private final Map<String, AdminUser> users = new ConcurrentHashMap<>();

        public AdminUserRepository() {
            seedUser("a", "Team A", "2026-01-15T10:00:00Z");
            seedUser("b", "Team B", "2026-01-16T10:00:00Z");
            seedUser("yarden", "Yarden", "2026-01-17T10:00:00Z");
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
            return users.values().stream()
                .filter(user -> user.getTeamName().equalsIgnoreCase(teamName))
                .map(this::copy)
                .toList();
        }

        public AdminUser save(AdminUser user) {
            users.put(user.getId(), copy(user));
            return copy(user);
        }

        public void delete(String id) {
            users.remove(id);
        }

        public void renameTeam(String oldTeamName, String newTeamName, String updatedAt) {
            List<AdminUser> affectedUsers = new ArrayList<>(findByTeamName(oldTeamName));
            affectedUsers.forEach(user -> save(new AdminUser(user.getId(), user.getUserId(), newTeamName, user.getCreatedAt(), updatedAt)));
        }

        private void seedUser(String userId, String teamName, String timestamp) {
            users.put(userId, new AdminUser(userId, userId, teamName, timestamp, timestamp));
        }

        private AdminUser copy(AdminUser user) {
            return new AdminUser(user.getId(), user.getUserId(), user.getTeamName(), user.getCreatedAt(), user.getUpdatedAt());
        }
    }

    public static String slugify(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    public static String nowIso() {
        return Instant.now().toString();
    }
}
