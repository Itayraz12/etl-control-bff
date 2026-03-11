package com.example.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Transformer {
    @JsonProperty("_id")
    private UUID id;
    private String name;
    private String description;
    private String format;
    private boolean canonize;

    @JsonProperty("isMultipleInput")
    private boolean multipleInput;

    // Values can be String or List<String> (e.g. "_required" key holds a string array).
    @JsonProperty("additionalProperties")
    private Map<String, Object> additionalProperties;


}
