package com.example.controller;

import com.example.model.AdminManagementDtos.SuccessResponse;
import com.example.model.AdminManagementDtos.TeamUpsertRequest;
import com.example.model.AdminManagementDtos.UdfApprovalRequest;
import com.example.model.AdminManagementDtos.UserUpsertRequest;
import com.example.model.AdminManagementDtos.AdminUserSummaryResponse;
import com.example.model.AdminManagementDtos.ErrorResponse;
import com.example.model.AdminTeam;
import com.example.model.AdminUser;
import com.example.model.Udf;
import com.example.service.admin.AdminManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backend/admin")
@Tag(name = "Admin Management", description = "Admin-only team and user management operations")
public class AdminManagementController {

    private static final Logger logger = LoggerFactory.getLogger(AdminManagementController.class);

    private final AdminManagementService adminManagementService;

    public AdminManagementController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    @GetMapping(value = "/teams", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all teams for the admin Team Management page")
    public List<AdminTeam> getTeams() {
        logger.info("Request arrived - GET /api/backend/admin/teams");
        return adminManagementService.getTeams();
    }

    @PostMapping(value = "/teams", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a team")
    public ResponseEntity<?> createTeam(@RequestBody TeamUpsertRequest request) {
        logger.info("Request arrived - POST /api/backend/admin/teams [teamName={}]", request.getTeamName());
        try {
            return ResponseEntity.ok(adminManagementService.createTeam(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping(value = "/teams/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a team")
    public ResponseEntity<?> updateTeam(@PathVariable String id, @RequestBody TeamUpsertRequest request) {
        logger.info("Request arrived - PUT /api/backend/admin/teams/{}", id);
        try {
            return ResponseEntity.ok(adminManagementService.updateTeam(id, request));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = "Team not found".equals(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping(value = "/teams/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a team")
    public ResponseEntity<?> deleteTeam(@PathVariable String id) {
        logger.info("Request arrived - DELETE /api/backend/admin/teams/{}", id);
        try {
            adminManagementService.deleteTeam(id);
            return ResponseEntity.ok(new SuccessResponse(true));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = "Team not found".equals(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all users for the admin User Management page")
    public List<AdminUser> getUsers() {
        logger.info("Request arrived - GET /api/backend/admin/users");
        return adminManagementService.getUsers();
    }

    @GetMapping(value = "/admin-users", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get admin users with userId and createdDate")
    public List<AdminUserSummaryResponse> getAdminUsers() {
        logger.info("Request arrived - GET /api/backend/admin/admin-users");
        return adminManagementService.getAdminUsers();
    }

    @GetMapping(value = "/udfs", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all UDFs for the admin UDF Management page")
    public List<Udf> getUdfs() {
        logger.info("Request arrived - GET /api/backend/admin/udfs");
        return adminManagementService.getUdfs();
    }

    @PutMapping(value = "/udfs/{udfId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update UDF approval status")
    public ResponseEntity<?> updateUdfApproval(@PathVariable String udfId, @RequestBody UdfApprovalRequest request) {
        logger.info("Request arrived - PUT /api/backend/admin/udfs/{} [isApproved={}]", udfId, request.getIsApproved());
        if (request.getIsApproved() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Bad Request", "isApproved is required"));
        }
        try {
            return ResponseEntity.ok(adminManagementService.updateUdfApproval(udfId, request.getIsApproved()));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = "UDF not found".equals(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(new ErrorResponse(status.getReasonPhrase(), ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Failed to update UDF approval", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal Server Error", "Failed to update UDF approval"));
        }
    }

    @DeleteMapping(value = "/udfs/{udfId}")
    @Operation(summary = "Delete a UDF")
    public ResponseEntity<?> deleteUdf(@PathVariable String udfId) {
        logger.info("Request arrived - DELETE /api/backend/admin/udfs/{}", udfId);
        try {
            adminManagementService.deleteUdf(udfId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Not Found", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Failed to delete UDF", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal Server Error", "Failed to delete UDF"));
        }
    }

    @PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a user")
    public ResponseEntity<?> createUser(@RequestBody UserUpsertRequest request) {
        logger.info("Request arrived - POST /api/backend/admin/users [userId={}]", request.getUserId());
        try {
            return ResponseEntity.ok(adminManagementService.createUser(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping(value = "/users/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a user")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody UserUpsertRequest request) {
        logger.info("Request arrived - PUT /api/backend/admin/users/{}", id);
        try {
            return ResponseEntity.ok(adminManagementService.updateUser(id, request));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = "User not found".equals(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("message", ex.getMessage()));
        }
    }

    @DeleteMapping(value = "/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete a user")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        logger.info("Request arrived - DELETE /api/backend/admin/users/{}", id);
        try {
            adminManagementService.deleteUser(id);
            return ResponseEntity.ok(new SuccessResponse(true));
        } catch (IllegalArgumentException ex) {
            HttpStatus status = "User not found".equals(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("message", ex.getMessage()));
        }
    }
}
