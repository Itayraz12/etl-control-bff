package com.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Filter {
    @JsonProperty("_id")
    private String id;
    private String name;
    private LocalDateTime createDate;
    private String description;
    private String owner;
    private String s3Path;
    private boolean approved;
    @JsonProperty("isActive")
    private boolean active;
    @JsonProperty("isRevertible")
    private boolean revertible;
    private String version;
    private List<AdditionalParam> additionalParams;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AdditionalParam {
        private String name;
        private String description;
        private String type;
        @JsonProperty("isArray")
        private boolean array;
    }
}
