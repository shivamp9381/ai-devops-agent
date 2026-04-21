package com.devops.agent.controller;

import com.devops.agent.model.ErrorRequest;
import com.devops.agent.service.AiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DebugController {

    private final AiService aiService;

    public DebugController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/debug")
    public String debug(@RequestBody ErrorRequest req) {
        return aiService.debugError(req.getError());
    }
}