package ru.agrofarm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Панель агронома (собранный React) раздаётся самим сервером из static/.
 * Адреса страниц панели перенаправляются на index.html, дальше маршрутизирует React.
 */
@Controller
public class SpaController {

    @GetMapping({"/", "/login", "/dashboard", "/farmers", "/farmers/{id}", "/units", "/anomalies",
            "/recommendations", "/reports"})
    public String index() {
        return "forward:/index.html";
    }
}
