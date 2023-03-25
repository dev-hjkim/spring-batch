package com.example.batch.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnrichmentController {
    private int count = 0;

    @GetMapping("/enrich")
    public String enrich() {
        this.count++;

        return String.format("Enriched %s", this.count);
    }
}
