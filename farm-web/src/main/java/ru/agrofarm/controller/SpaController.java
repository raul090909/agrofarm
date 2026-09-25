package ru.agrofarm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/", "/login", "/dashboard", "/farmers", "/farmers/{id}", "/units", "/anomalies",
            "/recommendations", "/reports"})
    public String index() {
        return "forward:/index.html";
    }
}
