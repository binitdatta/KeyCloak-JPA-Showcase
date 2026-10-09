package com.rollingstone.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthzTrainingController {

    @GetMapping("/training/fine-grained-authz-explained")
    public String show() {
        return "training/fine-grained-authz-explained";
    }
}