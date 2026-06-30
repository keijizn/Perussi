package com.perussi.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/teste")
    public String teste(Authentication authentication) {
        return "Usuário autenticado: " + authentication.getName();
    }
}