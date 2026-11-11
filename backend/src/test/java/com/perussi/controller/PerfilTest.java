package com.perussi.controller;

import com.perussi.dto.*;
import com.perussi.model.Usuario;
import com.perussi.repository.*;
import com.perussi.security.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PerfilTest {
    final UsuarioRepository repository = mock(UsuarioRepository.class);
    final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    final Usuario user = Usuario.builder().id(UUID.randomUUID()).email("perfil@example.com").nome("Teste")
            .senha(encoder.encode("original123")).role("CUSTOMER").build();
    final org.springframework.context.ApplicationEventPublisher eventos = mock(org.springframework.context.ApplicationEventPublisher.class);
    final UsuarioController controller = new UsuarioController(repository, mock(EnderecoUsuarioRepository.class), encoder, eventos);
    final JwtService jwt = new JwtService("test-secret-with-at-least-thirty-two-bytes");
    final SecurityUserDetailsService sessions = new SecurityUserDetailsService(repository);

    @BeforeEach void setup() {
        when(repository.findLockedByEmail("perfil@example.com")).thenReturn(Optional.of(user));
        when(repository.findByEmail("perfil@example.com")).thenAnswer(call -> Optional.of(user));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    }
    private UsuarioResponseDTO update(AtualizarUsuarioDTO dto) {
        return controller.atualizarPerfil(new UsernamePasswordAuthenticationToken("perfil@example.com", null), dto);
    }
    @Test void nameChangePreservesSessionAndReturnsUpdatedProfile() {
        user.setEmailConfirmado(true);
        String token = jwt.generateToken(user);
        assertEquals("Novo nome", update(new AtualizarUsuarioDTO("Novo nome", null, null, null)).nome());
        assertNotNull(sessions.loadSession(jwt.validateToken(token), token));
        assertTrue(user.getEmailConfirmado());
        verifyNoInteractions(eventos);
    }
    @Test void emailOrPasswordChangeRequiresCurrentPassword() {
        for (var dto : List.of(new AtualizarUsuarioDTO(null, "novo@example.com", null, null),
                new AtualizarUsuarioDTO(null, null, "nova123", "errada")))
            assertThrows(ResponseStatusException.class, () -> update(dto));
        verify(repository, never()).save(any());
        assertTrue(encoder.matches("original123", user.getSenha()));
    }
    @Test void passwordChangeInvalidatesOldSessionAndAcceptsNewLoginToken() {
        String old = jwt.generateToken(user);
        update(new AtualizarUsuarioDTO(null, null, "nova123", "original123"));
        assertTrue(encoder.matches("nova123", user.getSenha()));
        assertThrows(UsernameNotFoundException.class, () -> sessions.loadSession(jwt.validateToken(old), old));
        String fresh = jwt.generateToken(user);
        assertNotNull(sessions.loadSession(jwt.validateToken(fresh), fresh));
    }
    @Test void rejectsExistingEmailWithoutChangingUser() {
        when(repository.findByEmail("ocupado@example.com")).thenReturn(Optional.of(new Usuario()));
        assertThrows(ResponseStatusException.class, () -> update(new AtualizarUsuarioDTO("Outro", "ocupado@example.com", null, "original123")));
        assertEquals("perfil@example.com", user.getEmail());
        assertEquals("Teste", user.getNome());
        verify(repository, never()).save(any());
    }
    @Test void oldTokenCannotAccessAnotherAccountReusingEmail() {
        String old = jwt.generateToken(user);
        user.setId(UUID.randomUUID());
        assertThrows(UsernameNotFoundException.class, () -> sessions.loadSession(jwt.validateToken(old), old));
    }
    @Test void emailChangeRevokesSessionAndPreservesUserId() {
        user.setEmailConfirmado(true);
        user.setConfirmacaoHash("old-hash");
        user.setRecuperacaoHash("old-recovery");
        UUID id = user.getId();
        String old = jwt.generateToken(user);
        update(new AtualizarUsuarioDTO(null, "novo@example.com", null, "original123"));
        assertEquals(id, user.getId());
        assertEquals("novo@example.com", user.getEmail());
        assertFalse(user.getEmailConfirmado());
        assertNull(user.getConfirmacaoHash());
        assertNull(user.getRecuperacaoHash());
        verify(eventos).publishEvent(new com.perussi.service.confirmacao.ConfirmacaoEmailSolicitada(id));
        assertThrows(UsernameNotFoundException.class, () -> sessions.loadSession("perfil@example.com", old));
    }
    @Test void validatesEmptyProfileWeakPasswordAndInvalidAddress() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(new AtualizarUsuarioDTO(" ", "invalido", "123", null)).isEmpty());
            assertFalse(validator.validate(new EnderecoUsuarioDTO("abc", "", "", "", "", "XX", "")).isEmpty());
            assertTrue(validator.validate(new EnderecoUsuarioDTO("80020-000", "Rua", "1", "Centro", "Curitiba", "PR", "")).isEmpty());
        }
    }
}
