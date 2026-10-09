package com.rollingstone.demo.controller;

import com.rollingstone.demo.dto.UmaAuthzRunResult;
import com.rollingstone.demo.service.AuthzDemoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthzUiController {

    private final AuthzDemoService authzDemoService;

    public AuthzUiController(AuthzDemoService authzDemoService) {
        this.authzDemoService = authzDemoService;
    }

    @GetMapping("/training/fine-grained-authz-demo")
    public String show() {
        return "training/fine-grained-authz-demo";
    }

    @PostMapping("/training/fine-grained-authz-demo")
    public String run(Model model) {
        UmaAuthzRunResult result = authzDemoService.run();
        model.addAttribute("result", result);
        return "training/fine-grained-authz-demo";
    }
}