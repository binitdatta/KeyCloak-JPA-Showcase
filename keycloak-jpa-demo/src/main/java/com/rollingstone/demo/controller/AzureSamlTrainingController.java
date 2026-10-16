package com.rollingstone.demo.controller; // CHANGE to your application's controller package

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AzureSamlTrainingController {
    @GetMapping("/training/azure-saml")
    public String azureSaml() {
        return "training/azure-saml-walkthrough";
    }
}
