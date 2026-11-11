package com.perussi.controller;

import com.perussi.dto.AtualizarUsuarioDTO;
import com.perussi.dto.EnderecoUsuarioDTO;
import com.perussi.dto.UsuarioResponseDTO;
import com.perussi.model.EnderecoUsuario;
import com.perussi.model.Usuario;
import com.perussi.repository.EnderecoUsuarioRepository;
import com.perussi.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/usuarios")
@CrossOrigin("*")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final EnderecoUsuarioRepository enderecoUsuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final org.springframework.context.ApplicationEventPublisher eventos;

    public UsuarioController(
            UsuarioRepository usuarioRepository,
            EnderecoUsuarioRepository enderecoUsuarioRepository,
            BCryptPasswordEncoder passwordEncoder,
            org.springframework.context.ApplicationEventPublisher eventos
    ) {
        this.usuarioRepository = usuarioRepository;
        this.enderecoUsuarioRepository = enderecoUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventos = eventos;
    }

    @GetMapping("/me")
    public UsuarioResponseDTO buscarPerfil(Authentication authentication) {
        Usuario usuario = buscarUsuario(authentication.getName());

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole()
        );
    }

    @GetMapping("/me/endereco")
    public EnderecoUsuarioDTO buscarEndereco(Authentication authentication) {
        Usuario usuario = buscarUsuario(authentication.getName());

        EnderecoUsuario endereco = enderecoUsuarioRepository
                .findByUsuarioId(usuario.getId())
                .orElse(null);

        if (endereco == null) {
            return new EnderecoUsuarioDTO(
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            );
        }

        return new EnderecoUsuarioDTO(
                endereco.getCep(),
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getEstado(),
                endereco.getComplemento()
        );
    }

    @PutMapping("/me/endereco")
    public EnderecoUsuarioDTO atualizarEndereco(
            Authentication authentication,
            @jakarta.validation.Valid @RequestBody EnderecoUsuarioDTO dto
    ) {
        Usuario usuario = buscarUsuario(authentication.getName());

        EnderecoUsuario endereco = enderecoUsuarioRepository
                .findByUsuarioId(usuario.getId())
                .orElse(
                        EnderecoUsuario.builder()
                                .usuario(usuario)
                                .build()
                );

        endereco.setCep(dto.cep());
        endereco.setLogradouro(dto.logradouro());
        endereco.setNumero(dto.numero());
        endereco.setBairro(dto.bairro());
        endereco.setCidade(dto.cidade());
        endereco.setEstado(dto.estado());
        endereco.setComplemento(dto.complemento());

        EnderecoUsuario salvo = enderecoUsuarioRepository.save(endereco);

        return new EnderecoUsuarioDTO(
                salvo.getCep(),
                salvo.getLogradouro(),
                salvo.getNumero(),
                salvo.getBairro(),
                salvo.getCidade(),
                salvo.getEstado(),
                salvo.getComplemento()
        );
    }

    @PutMapping("/me")
    @org.springframework.transaction.annotation.Transactional
    public UsuarioResponseDTO atualizarPerfil(
            Authentication authentication,
            @jakarta.validation.Valid @RequestBody AtualizarUsuarioDTO dto
    ) {
        Usuario usuario = usuarioRepository.findLockedByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        boolean mudouEmail = dto.email() != null && !dto.email().equals(usuario.getEmail());
        boolean mudouSenha = dto.senha() != null;
        if ((mudouEmail || mudouSenha) && (dto.senhaAtual() == null
                || !passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a senha atual correta para alterar email ou senha.");

        if (mudouEmail) {
            boolean emailJaExiste = usuarioRepository.findByEmail(dto.email()).isPresent();

            if (emailJaExiste) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Email já cadastrado"
                );
            }

            usuario.setEmail(dto.email());
            usuario.setEmailConfirmado(false);
            usuario.setConfirmacaoHash(null);
            usuario.setConfirmacaoDestino(null);
            usuario.setConfirmacaoExpiraEm(null);
            usuario.setRecuperacaoHash(null);
            usuario.setRecuperacaoExpiraEm(null);
            eventos.publishEvent(new com.perussi.service.confirmacao.ConfirmacaoEmailSolicitada(usuario.getId()));
        }

        if (dto.nome() != null && !dto.nome().isBlank()) {
            usuario.setNome(dto.nome());
        }

        if (dto.senha() != null && !dto.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.senha()));
        }

        if (mudouEmail || mudouSenha) usuario.setVersaoSessao(java.util.UUID.randomUUID().toString());

        Usuario salvo = usuarioRepository.save(usuario);

        return new UsuarioResponseDTO(
                salvo.getId(),
                salvo.getNome(),
                salvo.getEmail(),
                salvo.getRole()
        );
    }

    @DeleteMapping("/me")
    public void excluirPerfil(Authentication authentication) {
        Usuario usuario = buscarUsuario(authentication.getName());
        usuarioRepository.delete(usuario);
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado"
                ));
    }
}
