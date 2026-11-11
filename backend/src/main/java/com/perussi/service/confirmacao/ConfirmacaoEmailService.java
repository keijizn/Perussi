package com.perussi.service.confirmacao;

import com.perussi.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ConfirmacaoEmailService {
    private final UsuarioRepository usuarios;
    private final ConfirmacaoEmail email;
    private final SecureRandom random = new SecureRandom();
    public ConfirmacaoEmailService(UsuarioRepository usuarios, ConfirmacaoEmail email) {
        this.usuarios = usuarios; this.email = email;
    }
    public record Situacao(String email, boolean confirmado, boolean envioDisponivel) {}
    public Situacao situacao(String endereco) {
        var usuario = usuarios.findByEmail(endereco).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new Situacao(usuario.getEmail(), Boolean.TRUE.equals(usuario.getEmailConfirmado()), email.habilitado());
    }
    public UUID usuarioId(String endereco) {
        return usuarios.findByEmail(endereco).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getId();
    }
    @Transactional
    public void solicitar(UUID id) {
        var usuario = usuarios.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (Boolean.TRUE.equals(usuario.getEmailConfirmado())) return;
        if (!email.habilitado()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Envio de confirmação temporariamente indisponível. Tente mais tarde.");
        Instant agora = Instant.now();
        if (usuario.getConfirmacaoSolicitadaEm() != null && usuario.getConfirmacaoSolicitadaEm().plusSeconds(60).isAfter(agora))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Aguarde um minuto antes de pedir outro link.");
        if (usuario.getConfirmacaoJanelaEm() == null || !usuario.getConfirmacaoJanelaEm().plusSeconds(900).isAfter(agora)) {
            usuario.setConfirmacaoJanelaEm(agora);
            usuario.setConfirmacaoEnvios(0);
        }
        int envios = Optional.ofNullable(usuario.getConfirmacaoEnvios()).orElse(0);
        if (envios >= 5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas solicitações. Aguarde 15 minutos.");
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        usuario.setConfirmacaoHash(hash(token));
        usuario.setConfirmacaoDestino(usuario.getEmail());
        usuario.setConfirmacaoExpiraEm(agora.plusSeconds(1800));
        usuario.setConfirmacaoSolicitadaEm(agora);
        usuario.setConfirmacaoEnvios(envios + 1);
        try { email.enviar(usuario.getEmail(), token); }
        catch (Exception e) {
            // Do not log provider bodies: they can contain the link and recipient.
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Falha no envio de confirmação ({})", e.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível enviar o link. Tente novamente mais tarde.");
        }
    }
    @Transactional
    public void confirmar(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalido();
        String hash = hash(token);
        var id = usuarios.findIdByConfirmacaoHash(hash).orElseThrow(this::invalido);
        var usuario = usuarios.findLockedById(id).orElseThrow(this::invalido);
        if (!hash.equals(usuario.getConfirmacaoHash()) || Boolean.TRUE.equals(usuario.getEmailConfirmado())
                || usuario.getConfirmacaoExpiraEm() == null || !usuario.getConfirmacaoExpiraEm().isAfter(Instant.now())
                || !usuario.getEmail().equals(usuario.getConfirmacaoDestino())) throw invalido();
        usuario.setEmailConfirmado(true);
        usuario.setConfirmacaoHash(null);
        usuario.setConfirmacaoDestino(null);
        usuario.setConfirmacaoExpiraEm(null);
    }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link inválido, expirado ou já utilizado. Entre na sua conta para pedir outro.");
    }
}
