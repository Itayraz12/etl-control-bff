package com.example.model;

public class Transfer {
    private String id;
    private String source;
    private String destination;

    public Transfer() {}

    public Transfer(String id, String source, String destination) {
        this.id = id;
        this.source = source;
        this.destination = destination;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    @Override
    public String toString() {
        return "Transfer{" +
                "id='" + id + '\'' +
                ", source='" + source + '\'' +
                ", destination='" + destination + '\'' +
                '}';
    }
}

