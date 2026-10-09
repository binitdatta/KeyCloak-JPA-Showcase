package com.rollingstone.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.rollingstone.demo.service.DeviceGrantDemoService;

@Controller
public class DeviceGrantUiController {

    private final DeviceGrantDemoService service;

    public DeviceGrantUiController(DeviceGrantDemoService service) {
        this.service = service;
    }

    @GetMapping("/training/device-grant-demo")
    public String page(Model model) {
        model.addAttribute("clientId", service.getClientId());
        model.addAttribute("deviceEndpoint", service.getDeviceEndpoint());
        model.addAttribute("tokenEndpoint", service.getTokenEndpoint());
        return "training/device-grant-demo";
    }

    @PostMapping("/training/device-grant-demo/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> start(@RequestBody Map<String, String> req) {
        return ResponseEntity.ok(service.requestCodes(req.getOrDefault("scope", "openid")));
    }

    @PostMapping("/training/device-grant-demo/poll")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> poll(@RequestBody Map<String, String> req) {
        return ResponseEntity.ok(service.poll(req.get("deviceCode")));
    }
}
