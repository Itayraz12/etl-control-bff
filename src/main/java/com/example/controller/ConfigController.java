package com.example.controller;

import com.example.model.ConfigOption;
import com.example.model.Filter;
import com.example.model.Transformer;
import com.example.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@Tag(name = "Config Service", description = "Configuration service operations")
public class ConfigController {
    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    @Autowired
    private ConfigService configService;

    @GetMapping("/transformers")
    @Operation(summary = "Get all transforers")
    public List<Transformer> getTransforers() {
        logger.info("Request arrived - GET /api/config/transforers");
        List<Transformer> response = configService.getTransforers();
        logger.info("Response payload: {} transforers returned", response.size());
        logger.debug("Response details: {}", response);
        return response;
    }

    @GetMapping("/filters")
    @Operation(summary = "Get all filters")
    public List<Filter> getFilters() {
        logger.info("Request arrived - GET /api/config/filters");
        List<Filter> response = configService.getFilters();
        logger.info("Response payload: {} filters returned", response.size());
        logger.debug("Response details: {}", response);
        return response;
    }

    @GetMapping("/streaming-continuities")
    @Operation(summary = "Get streaming continuity options")
    public List<ConfigOption> getStreamingContinuities() {
        logger.info("Request arrived - GET /api/config/streaming-continuities");
        List<ConfigOption> response = configService.getStreamingContinuities();
        logger.info("Response payload: {} streaming continuity options returned", response.size());
        logger.debug("Response details: {}", response);
        return response;
    }

    @GetMapping("/records-per-day")
    @Operation(summary = "Get average records per day options")
    public List<ConfigOption> getRecordsPerDayOptions() {
        logger.info("Request arrived - GET /api/config/records-per-day");
        List<ConfigOption> response = configService.getRecordsPerDayOptions();
        logger.info("Response payload: {} records-per-day options returned", response.size());
        logger.debug("Response details: {}", response);
        return response;
    }
}
