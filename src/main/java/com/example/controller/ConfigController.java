package com.example.controller;

import com.example.model.Transfer;
import com.example.model.Filter;
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

    @GetMapping("/transforers")
    @Operation(summary = "Get all transforers")
    public List<Transfer> getTransforers() {
        logger.info("Request arrived - GET /api/config/transforers");
        List<Transfer> response = configService.getTransforers();
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
}
