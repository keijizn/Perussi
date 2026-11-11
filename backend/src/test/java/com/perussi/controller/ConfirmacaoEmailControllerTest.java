package com.perussi.controller;

import com.perussi.config.SecurityConfig;
import com.perussi.security.*;
import com.perussi.service.confirmacao.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConfirmacaoEmailController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, LimiteConfirmacao.class})
class ConfirmacaoEmailControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ConfirmacaoEmailService service;
    @MockBean JwtService jwt;
    @MockBean SecurityUserDetailsService users;
    @Test void anonymousCannotResendOrReadConfirmationState() throws Exception {
        mvc.perform(get("/usuarios/me/confirmacao-email")).andExpect(status().isUnauthorized());
        mvc.perform(post("/usuarios/me/confirmacao-email")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void confirmationLinkWorksWithoutLoginEvenWithExpiredBearer() throws Exception {
        String token="A".repeat(43);
        mvc.perform(post("/auth/confirmar-email").servletPath("/auth/confirmar-email")
                .header("Authorization", "Bearer expired").contentType("application/json")
                .content("{\"token\":\""+token+"\"}")).andExpect(status().isOk());
        verify(service).confirmar(token);
        verifyNoInteractions(jwt);
    }
    @Test void malformedTokenDoesNotReachService() throws Exception {
        mvc.perform(post("/auth/confirmar-email").contentType("application/json")
                .content("{\"token\":\"invalid\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test @WithMockUser(username="dono@example.com")
    void resendUsesOnlyAuthenticatedIdentity() throws Exception {
        UUID id=UUID.randomUUID();
        when(service.usuarioId("dono@example.com")).thenReturn(id);
        mvc.perform(post("/usuarios/me/confirmacao-email").contentType("application/json")
                .content("{\"email\":\"outra@example.com\"}")).andExpect(status().isOk());
        verify(service).usuarioId("dono@example.com");
        verify(service).solicitar(id);
        verify(service, never()).usuarioId("outra@example.com");
    }
    @Test @WithMockUser(username="dono@example.com")
    void statusExposesNoToken() throws Exception {
        when(service.situacao("dono@example.com")).thenReturn(new ConfirmacaoEmailService.Situacao("dono@example.com", false, true));
        mvc.perform(get("/usuarios/me/confirmacao-email")).andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmado").value(false)).andExpect(jsonPath("$.token").doesNotExist());
    }
}
