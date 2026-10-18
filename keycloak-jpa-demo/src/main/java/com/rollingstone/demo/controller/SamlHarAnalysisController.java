// Change the package to match the existing controllers in your application.
package com.rollingstone.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SamlHarAnalysisController {
    @GetMapping("/training/saml-har-analysis")
    public String page() {
        return "training/saml-har-analysis";
    }
}
