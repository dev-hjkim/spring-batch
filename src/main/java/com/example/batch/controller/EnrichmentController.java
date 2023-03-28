package com.example.batch.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnrichmentController {
    private int count = 0;

    @GetMapping("/enrich")
    public String enrich() {
        if (Math.random() > .5) {
            throw new RuntimeException("I screwed up");
        } else {
            this.count++;

            return String.format("Enriched %s", this.count);
        }
    }
}
