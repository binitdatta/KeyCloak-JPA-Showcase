package com.rollingstone.demo.controller;

import com.rollingstone.demo.dto.SignedJwtRunResult;
import com.rollingstone.demo.service.SignedJwtDemoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * UI-facing counterpart to SignedJwtDemoController (the JSON REST API):
 * GET renders the empty "Try it live" page; POST runs the flow server-side
 * and re-renders the same view with the result on the model — the classic
 * MVC round-trip, no JavaScript involved.
 */
@Controller
public class SignedJwtUiController {

    private final SignedJwtDemoService signedJwtDemoService;

    public SignedJwtUiController(SignedJwtDemoService signedJwtDemoService) {
        this.signedJwtDemoService = signedJwtDemoService;
    }

    @GetMapping("/training/signed-jwt-demo")
    public String show() {
        return "training/signed-jwt-demo";
    }

    @PostMapping("/training/signed-jwt/run")
    public String run(Model model) {
        SignedJwtRunResult result = signedJwtDemoService.run();
        model.addAttribute("result", result);
        return "training/signed-jwt-demo";
    }
}