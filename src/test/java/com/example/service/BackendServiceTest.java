package com.example.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackendServiceTest {

    private final BackendService backendService = new BackendService("saved-configurations");

    @Test
    void getSchemaFromPayload_shouldReturnFlatSchemaForCsvFormatType() {
        String schema = backendService.getSchemaFromPayload("payload-body", "CSV");

        assertEquals(backendService.getSchemaByName("personFlat"), schema);
    }

    @Test
    void getSchemaFromPayload_shouldReturnObjectSchemaForJsonFormatType() {
        String schema = backendService.getSchemaFromPayload("payload-body", "JSON");

        assertEquals(backendService.getSchemaByName("personObject"), schema);
    }

    @Test
    void getSchemaFromPayload_shouldRejectUnsupportedFormatType() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> backendService.getSchemaFromPayload("payload-body", "XML")
        );

        assertEquals("Unsupported formatType: XML", exception.getMessage());
    }

    @Test
    void getAllDeployments_shouldReturnDeploymentsAcrossAllTeams() {
        var deployments = backendService.getAllDeployments();

        assertEquals(15, deployments.size());
        assertEquals("1", deployments.get(0).getId());
        assertEquals("Team A", deployments.get(0).getTeamName());
        assertTrue(deployments.get(0).getLastStatusChange().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
        assertTrue(deployments.get(0).getCreatedAt().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
        assertEquals("12", deployments.get(deployments.size() - 1).getId());
        assertEquals("Team D1", deployments.get(deployments.size() - 1).getTeamName());
    }

    @Test
    void getDeployments_shouldReturnEmptyListForUnknownTeam() {
        var deployments = backendService.getDeployments("Unknown Team");

        assertTrue(deployments.isEmpty());
    }
}

