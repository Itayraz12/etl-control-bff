package com.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Filter {
    private String id;
    private String name;
    private String rule;
    @JsonProperty("isRevertible")
    private boolean revertible;

    @JsonProperty("isInclude")
    private boolean include;
}
