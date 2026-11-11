package com.perussi.controller;

import com.perussi.service.confirmacao.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class ConfirmacaoEmailController {
    private final ConfirmacaoEmailService service;
    private final LimiteConfirmacao limite;
    public ConfirmacaoEmailController(ConfirmacaoEmailService service, LimiteConfirmacao limite) {
        this.service = service; this.limite = limite;
    }
    public record Confirmar(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token) {}
    @GetMapping("/usuarios/me/confirmacao-email")
    public ConfirmacaoEmailService.Situacao situacao(Authentication auth) { return service.situacao(auth.getName()); }
    @PostMapping("/usuarios/me/confirmacao-email")
    public Map<String, String> reenviar(Authentication auth) {
        limite.verificar("reenviar:" + auth.getName(), 10);
        service.solicitar(service.usuarioId(auth.getName()));
        return Map.of("message", "Se o e-mail ainda não estiver confirmado, enviamos um link. Verifique também o spam.");
    }
    @PostMapping("/auth/confirmar-email")
    public Map<String, String> confirmar(@Valid @RequestBody Confirmar dto, HttpServletRequest request) {
        limite.verificar("confirmar:" + request.getRemoteAddr(), 30);
        service.confirmar(dto.token());
        return Map.of("message", "E-mail confirmado! Você já pode finalizar suas compras.");
    }
}
