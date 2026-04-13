package com.example.service.admin;

import com.example.model.AdminManagementDtos.TeamUpsertRequest;
import com.example.model.AdminManagementDtos.UserUpsertRequest;
import com.example.model.AdminTeam;
import com.example.model.AdminUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.service.admin.AdminRepositories.nowIso;
import static com.example.service.admin.AdminRepositories.slugify;

@Service
public class AdminManagementService {

    private final AdminRepositories.AdminTeamRepository teamRepository;
    private final AdminRepositories.AdminUserRepository userRepository;

    public AdminManagementService(AdminRepositories.AdminTeamRepository teamRepository,
                                  AdminRepositories.AdminUserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    public List<AdminTeam> getTeams() {
        return teamRepository.findAll();
    }

    public AdminTeam createTeam(TeamUpsertRequest request) {
        validateRequired("teamName", request.getTeamName());
        validateRequired("devopsName", request.getDevopsName());
        if (teamRepository.existsByTeamName(request.getTeamName())) {
            throw new IllegalArgumentException("Team name already exists");
        }

        String timestamp = nowIso();
        AdminTeam team = new AdminTeam(slugify(request.getTeamName()), request.getTeamName().trim(), request.getDevopsName().trim(), timestamp, timestamp);
        return teamRepository.save(team);
    }

    public AdminTeam updateTeam(String id, TeamUpsertRequest request) {
        AdminTeam existingTeam = teamRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Team not found"));

        validateRequired("teamName", request.getTeamName());
        validateRequired("devopsName", request.getDevopsName());

        teamRepository.findByTeamName(request.getTeamName())
            .filter(team -> !team.getId().equals(existingTeam.getId()))
            .ifPresent(team -> {
                throw new IllegalArgumentException("Team name already exists");
            });

        String newId = slugify(request.getTeamName());
        String updatedAt = nowIso();
        AdminTeam updatedTeam = new AdminTeam(newId, request.getTeamName().trim(), request.getDevopsName().trim(), existingTeam.getCreatedAt(), updatedAt);

        if (!existingTeam.getId().equals(newId)) {
            teamRepository.delete(existingTeam.getId());
        }
        teamRepository.save(updatedTeam);
        userRepository.renameTeam(existingTeam.getTeamName(), updatedTeam.getTeamName(), updatedAt);
        return updatedTeam;
    }

    public void deleteTeam(String id) {
        AdminTeam existingTeam = teamRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Team not found"));
        if (!userRepository.findByTeamName(existingTeam.getTeamName()).isEmpty()) {
            throw new IllegalArgumentException("Cannot delete team with assigned users");
        }
        teamRepository.delete(id);
    }

    public List<AdminUser> getUsers() {
        return userRepository.findAll();
    }

    public AdminUser createUser(UserUpsertRequest request) {
        validateRequired("userId", request.getUserId());
        validateRequired("teamName", request.getTeamName());
        validateTeamExists(request.getTeamName());
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("User ID already exists");
        }

        String timestamp = nowIso();
        AdminUser user = new AdminUser(request.getUserId().trim(), request.getUserId().trim(), resolveCanonicalTeamName(request.getTeamName()), timestamp, timestamp);
        return userRepository.save(user);
    }

    public AdminUser updateUser(String id, UserUpsertRequest request) {
        AdminUser existingUser = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateRequired("userId", request.getUserId());
        validateRequired("teamName", request.getTeamName());
        validateTeamExists(request.getTeamName());

        userRepository.findByUserId(request.getUserId())
            .filter(user -> !user.getId().equals(existingUser.getId()))
            .ifPresent(user -> {
                throw new IllegalArgumentException("User ID already exists");
            });

        String newId = request.getUserId().trim();
        AdminUser updatedUser = new AdminUser(newId, newId, resolveCanonicalTeamName(request.getTeamName()), existingUser.getCreatedAt(), nowIso());
        if (!existingUser.getId().equals(newId)) {
            userRepository.delete(existingUser.getId());
        }
        return userRepository.save(updatedUser);
    }

    public void deleteUser(String id) {
        userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.delete(id);
    }

    private void validateTeamExists(String teamName) {
        if (teamRepository.findByTeamName(teamName).isEmpty()) {
            throw new IllegalArgumentException("Team name must reference an existing team");
        }
    }

    private String resolveCanonicalTeamName(String teamName) {
        return teamRepository.findByTeamName(teamName)
            .map(AdminTeam::getTeamName)
            .orElse(teamName.trim());
    }

    private void validateRequired(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
