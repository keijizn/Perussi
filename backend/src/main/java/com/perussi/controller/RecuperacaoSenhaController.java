package com.perussi.controller;

import com.perussi.service.*;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/auth")
public class RecuperacaoSenhaController {
    public record Solicitar(@NotBlank @Email @Size(max=254) String email) {}
    public record Redefinir(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,
                           @NotBlank @Size(min=6, max=72) String senha) {}
    private final RecuperacaoSenhaService service;
    private final RecuperacaoEmail email;
    private final ThreadPoolExecutor fila = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(50));
    private final Map<String, Janela> limites = new HashMap<>();
    private record Janela(long inicio, int tentativas) {}

    public RecuperacaoSenhaController(RecuperacaoSenhaService service, RecuperacaoEmail email) {
        this.service = service; this.email = email;
    }

    @PostMapping("/esqueci-senha")
    public Map<String, String> solicitar(@Valid @RequestBody Solicitar dto, HttpServletRequest request) {
        if (!email.habilitado()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Recuperação de senha temporariamente indisponível.");
        limitar("solicitar:" + request.getRemoteAddr(), 5);
        try {
            fila.execute(() -> {
                try { service.solicitar(dto.email()); }
                catch (org.springframework.web.client.RestClientResponseException e) {
                    String resposta = e.getResponseBodyAsString().toLowerCase(Locale.ROOT);
                    String motivo = List.of("private key", "public key", "access token", "non-browser",
                            "service id", "template id", "origin", "quota", "recipient")
                            .stream().filter(resposta::contains).findFirst().orElse("não identificado");
                    LoggerFactory.getLogger(getClass()).warn("EmailJS respondeu HTTP {} ({}).",
                            e.getStatusCode().value(), motivo);
                }
                catch (org.springframework.web.client.ResourceAccessException e) {
                    LoggerFactory.getLogger(getClass()).warn("Falha de conexão com EmailJS ({}).",
                            e.getMostSpecificCause().getClass().getSimpleName());
                }
                catch (Exception e) {
                    // Mail exceptions can contain addresses and links. Never log their contents.
                    LoggerFactory.getLogger(getClass()).warn("Falha no envio de recuperação de senha ({}).",
                            e.getClass().getSimpleName());
                }
            });
        } catch (RejectedExecutionException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Tente novamente em alguns minutos.");
        }
        return Map.of("message", "Se este email estiver cadastrado, enviaremos um link de recuperação. Verifique também o spam.");
    }

    @PostMapping("/redefinir-senha")
    public Map<String, String> redefinir(@Valid @RequestBody Redefinir dto, HttpServletRequest request) {
        limitar("redefinir:" + request.getRemoteAddr(), 20);
        service.redefinir(dto.token(), dto.senha());
        return Map.of("message", "Senha alterada. Entre novamente com sua nova senha.");
    }

    private synchronized void limitar(String chave, int maximo) {
        long agora = System.currentTimeMillis();
        limites.entrySet().removeIf(e -> agora - e.getValue().inicio() >= 900_000);
        Janela janela = limites.get(chave);
        if (janela == null && limites.size() >= 10_000 || janela != null && janela.tentativas() >= maximo)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde 15 minutos.");
        limites.put(chave, new Janela(janela == null ? agora : janela.inicio(), janela == null ? 1 : janela.tentativas() + 1));
    }

    @PreDestroy public void fechar() { fila.shutdown(); }
}
