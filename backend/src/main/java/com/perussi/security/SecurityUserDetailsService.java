package com.perussi.security;

import com.perussi.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public SecurityUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email)
                .map(usuario -> org.springframework.security.core.userdetails.User
                        .withUsername(usuario.getEmail())
                        .password(usuario.getSenha())
                        .authorities(List.of(new SimpleGrantedAuthority(usuario.getRole())))
                        .build()
                )
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    // Called only after JwtService verifies the signature, issuer and expiry.
    public UserDetails loadSession(String email, String token) {
        var usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Sessão inválida"));
        var claims = com.auth0.jwt.JWT.decode(token);
        String version = usuario.getVersaoSessao() == null ? "0" : usuario.getVersaoSessao();
        if (!usuario.getId().toString().equals(claims.getClaim("uid").asString())
                || !version.equals(claims.getClaim("session").asString()))
            throw new UsernameNotFoundException("Sessão invalidada. Entre novamente.");
        return org.springframework.security.core.userdetails.User.withUsername(usuario.getEmail())
                .password(usuario.getSenha()).authorities(usuario.getRole()).build();
    }
}
