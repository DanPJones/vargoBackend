package com.example.vargoBackend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.vargoBackend.service.RoundGenerator;

@RestController
@RequestMapping("/rounds")
public class RouletteController {

    private final RoundGenerator rouletteGenerator;

    public RouletteController(RoundGenerator rouletteGenerator) {
        this.rouletteGenerator = rouletteGenerator;  
    }

    @GetMapping("/test")
    public int getOne() {
        return 1;
    }

    @GetMapping("/getRollTime")
    public Map<String, Long> getTime() {
        return Map.of("time", rouletteGenerator.current().rollTime());
    }

    @GetMapping("/getRollPx")
    public Map<String, Integer> getRollPx() {
        return Map.of("px", rouletteGenerator.current().rollPx());
    }

    @GetMapping("/getNow")
    public Map<String, Long> getNow() {
        return Map.of("now", System.currentTimeMillis());
    }

}
