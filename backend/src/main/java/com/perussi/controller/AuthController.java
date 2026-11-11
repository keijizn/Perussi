package com.perussi.controller;

import com.perussi.dto.LoginDTO;
import com.perussi.dto.LoginResponseDTO;
import com.perussi.dto.UsuarioRegisterDTO;
import com.perussi.dto.UsuarioResponseDTO;
import com.perussi.model.Usuario;
import com.perussi.repository.UsuarioRepository;
import com.perussi.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@CrossOrigin("*")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final org.springframework.context.ApplicationEventPublisher eventos;
    private final com.perussi.service.confirmacao.LimiteConfirmacao limite;

    public AuthController(
            UsuarioRepository usuarioRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            org.springframework.context.ApplicationEventPublisher eventos,
            com.perussi.service.confirmacao.LimiteConfirmacao limite
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventos = eventos;
        this.limite = limite;
    }

    @PostMapping("/register")
    @org.springframework.transaction.annotation.Transactional
    public UsuarioResponseDTO register(@Valid @RequestBody UsuarioRegisterDTO dto, jakarta.servlet.http.HttpServletRequest request) {
        limite.verificar("cadastro:" + request.getRemoteAddr(), 10);
        if (usuarioRepository.findByEmail(dto.email()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email já cadastrado"
            );
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(passwordEncoder.encode(dto.senha()))
                .role("CUSTOMER")
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        eventos.publishEvent(new com.perussi.service.confirmacao.ConfirmacaoEmailSolicitada(usuarioSalvo.getId()));

        return new UsuarioResponseDTO(
                usuarioSalvo.getId(),
                usuarioSalvo.getNome(),
                usuarioSalvo.getEmail(),
                usuarioSalvo.getRole()
        );
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Email ou senha inválidos"
                ));

        boolean senhaCorreta = passwordEncoder.matches(dto.senha(), usuario.getSenha());

        if (!senhaCorreta) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email ou senha inválidos"
            );
        }

        String token = jwtService.generateToken(usuario);

        return new LoginResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                token
        );
    }
}