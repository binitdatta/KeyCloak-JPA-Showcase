package com.rollingstone.demo.controller;

import com.rollingstone.demo.dto.X509RunResult;
import com.rollingstone.demo.service.X509DemoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class X509UiController {

    private final X509DemoService x509DemoService;

    public X509UiController(X509DemoService x509DemoService) {
        this.x509DemoService = x509DemoService;
    }

    @GetMapping("/training/x509-certificate-demo")
    public String show() {
        return "training/x509-certificate-demo";
    }

    @GetMapping("/training/x509-mtls-concepts-java-commands")
    public String explain() {
        return "training/x509-mtls-concepts-java-commands";
    }



    @PostMapping("/training/x509-certificate-demo/run")
    public String run(Model model) {
        X509RunResult result = x509DemoService.run();
        model.addAttribute("result", result);
        return "training/x509-certificate-demo";
    }
}