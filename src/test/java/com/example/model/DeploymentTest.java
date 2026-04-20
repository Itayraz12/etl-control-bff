package com.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentTest {

    @Test
    void deployment_shouldStoreTeamNameFromConstructor() {
        Deployment deployment = new Deployment("1", "Team A", "ETL Job", "Bitbucket", "running",
            "2.1.3", "2.0.5", "2026-05-15T12:58:56.323", "2026-05-14T12:58:56.323", "CAP");

        assertEquals("Team A", deployment.getTeamName());
        assertTrue(deployment.toString().contains("teamName='Team A'"));
    }

    @Test
    void deployment_shouldUpdateTeamNameViaSetter() {
        Deployment deployment = new Deployment();

        deployment.setTeamName("Team B");

        assertEquals("Team B", deployment.getTeamName());
    }
}
