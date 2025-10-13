package com.gourav.LedgerLens.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Value("${google.api.key}")
    private String Genkey;

    @GetMapping("/home")
    public String greet() {

        return Genkey;
    }
}
