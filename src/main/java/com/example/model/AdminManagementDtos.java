package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public final class AdminManagementDtos {

    private AdminManagementDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class TeamUpsertRequest {
        private String teamName;
        private String devopsName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class UserUpsertRequest {
        private String userId;
        private String teamName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class SuccessResponse {
        private boolean success;
    }
}
