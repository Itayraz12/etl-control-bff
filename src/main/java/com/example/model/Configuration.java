package com.example.model;

public class Configuration {
    private String id;
    private String name;
    private String settings;

    public Configuration() {}

    public Configuration(String id, String name, String settings) {
        this.id = id;
        this.name = name;
        this.settings = settings;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSettings() { return settings; }
    public void setSettings(String settings) { this.settings = settings; }

    @Override
    public String toString() {
        return "Configuration{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", settings='" + settings + '\'' +
                '}';
    }
}

