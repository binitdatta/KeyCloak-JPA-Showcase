package com.rollingstone.demo.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.rollingstone.demo.service.CibaDemoService;

@Controller
public class CibaUiController {

    private final CibaDemoService service;

    public CibaUiController(CibaDemoService service) {
        this.service = service;
    }

    @GetMapping("/training/ciba-demo")
    public String page(Model model) {
        model.addAttribute("clientId", service.getClientId());
        model.addAttribute("authEndpoint", service.getAuthEndpoint());
        model.addAttribute("tokenEndpoint", service.getTokenEndpoint());
        return "training/ciba-demo";
    }

    @PostMapping("/training/ciba-demo/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> start(@RequestBody Map<String, String> req) {
        return ResponseEntity.ok(service.initiate(
                req.getOrDefault("loginHint", "alice"),
                req.getOrDefault("scope", "openid"),
                req.get("bindingMessage")));
    }

    @PostMapping("/training/ciba-demo/poll")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> poll(@RequestBody Map<String, String> req) {
        return ResponseEntity.ok(service.poll(req.get("authReqId")));
    }
}
