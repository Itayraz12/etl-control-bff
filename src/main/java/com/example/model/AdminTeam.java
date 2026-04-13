package com.example.model;

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
public class AdminTeam {
    private String id;
    private String teamName;
    private String devopsName;
    private String createdAt;
    private String updatedAt;
}
