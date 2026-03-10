package com.example.controller;

import com.example.model.Entity;
import com.example.service.BackboneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backbone")
@Tag(name = "Backbone Service", description = "Backbone service operations")
public class BackboneController {

    @Autowired
    private BackboneService backboneService;

    @GetMapping("/entity/{id}")
    @Operation(summary = "Get entity by ID")
    public Entity getEntity(@PathVariable String id) {
        return backboneService.getEntity(id);
    }
}

