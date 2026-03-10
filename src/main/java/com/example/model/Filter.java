package com.example.model;

public class Filter {
    private String id;
    private String name;
    private String rule;

    public Filter() {}

    public Filter(String id, String name, String rule) {
        this.id = id;
        this.name = name;
        this.rule = rule;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }

    @Override
    public String toString() {
        return "Filter{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", rule='" + rule + '\'' +
                '}';
    }
}

