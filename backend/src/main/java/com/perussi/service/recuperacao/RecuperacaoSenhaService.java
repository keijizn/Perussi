package com.perussi.service.recuperacao;

import com.perussi.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class RecuperacaoSenhaService {
    private final UsuarioRepository usuarios;
    private final BCryptPasswordEncoder encoder;
    private final RecuperacaoEmail email;
    private final SecureRandom random = new SecureRandom();

    public RecuperacaoSenhaService(UsuarioRepository usuarios, BCryptPasswordEncoder encoder, RecuperacaoEmail email) {
        this.usuarios = usuarios; this.encoder = encoder; this.email = email;
    }

    @Transactional
    public void solicitar(String endereco) {
        if (!email.habilitado()) return;
        var usuario = usuarios.findLockedByEmail(endereco.trim()).orElse(null);
        if (usuario == null) return;
        Instant agora = Instant.now();
        if (usuario.getRecuperacaoSolicitadaEm() != null && usuario.getRecuperacaoSolicitadaEm().plusSeconds(60).isAfter(agora)) return;
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        usuario.setRecuperacaoHash(hash(token));
        usuario.setRecuperacaoExpiraEm(agora.plusSeconds(900));
        usuario.setRecuperacaoSolicitadaEm(agora);
        usuario.setRecuperacaoVersaoSessao(usuario.getVersaoSessao());
        // The raw token is sent only in the email, never returned or logged.
        email.enviar(usuario.getEmail(), token);
    }

    @Transactional
    public void redefinir(String token, String senha) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalido();
        if (senha == null || senha.isBlank() || senha.length() < 6 || senha.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 6 caracteres e no máximo 72 bytes.");
        String hash = hash(token);
        var id = usuarios.findIdByRecuperacaoHash(hash).orElseThrow(this::invalido);
        var usuario = usuarios.findLockedById(id).orElseThrow(this::invalido);
        // The scalar lookup avoids loading stale entity state before acquiring the lock.
        if (!hash.equals(usuario.getRecuperacaoHash()) || usuario.getRecuperacaoExpiraEm() == null
                || !usuario.getRecuperacaoExpiraEm().isAfter(Instant.now())
                || !Objects.equals(usuario.getVersaoSessao(), usuario.getRecuperacaoVersaoSessao())) throw invalido();
        usuario.setSenha(encoder.encode(senha));
        usuario.setVersaoSessao(UUID.randomUUID().toString());
        usuario.setRecuperacaoHash(null);
        usuario.setRecuperacaoExpiraEm(null);
        usuario.setRecuperacaoVersaoSessao(null);
    }

    static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link inválido, expirado ou já utilizado. Solicite um novo link.");
    }
}
