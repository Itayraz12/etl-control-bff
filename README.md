# etl-control-bff

Backend For Frontend (BFF) service responsible for orchestrating ETL configuration by integrating Kafka, Schema Registry, and external services, and generating pipeline configuration artifacts.

## Overview

**etl-control-bff** is a Java-based Backend For Frontend service that acts as the control layer for ETL pipeline configuration.

The service exposes REST APIs consumed by the ETL configuration UI and is responsible for orchestrating interactions with multiple backend systems such as Kafka brokers, schema registries, and additional platform services. It aggregates metadata from these systems and generates configuration artifacts (such as YAML pipeline definitions) used to deploy and run ETL pipelines.

## Key Responsibilities

- Provide REST APIs for the ETL configuration UI
- Retrieve metadata from Kafka brokers using the Kafka AdminClient
- Fetch schemas and subject information from Schema Registry (Confluent or Apicurio)
- Integrate with additional platform services
- Validate user configuration inputs
- Generate pipeline configuration files (YAML)
- Act as a secure abstraction layer between the UI and infrastructure systems

## Architecture

## Technology Stack

- Java
- Spring Boot
- Kafka AdminClient
- Schema Registry Client (Confluent / Apicurio)
- Jackson Dataformat YAML or SnakeYAML
- REST APIs

## Purpose

The BFF ensures that infrastructure connectivity, credential management, metadata retrieval, and configuration generation are handled securely in the backend rather than in the browser.
