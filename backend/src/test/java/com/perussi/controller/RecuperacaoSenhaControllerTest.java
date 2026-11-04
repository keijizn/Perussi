package com.perussi.controller;

import com.perussi.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecuperacaoSenhaControllerTest {
    @Test void uniformResponseAndRateLimit() {
        var service=mock(RecuperacaoSenhaService.class);
        var email=mock(RecuperacaoEmail.class); when(email.habilitado()).thenReturn(true);
        var controller=new RecuperacaoSenhaController(service,email);
        var request=new MockHttpServletRequest(); request.setRemoteAddr("127.0.0.1");
        try {
            var conhecida=controller.solicitar(new RecuperacaoSenhaController.Solicitar("cliente@example.com"),request);
            assertEquals(conhecida,controller.solicitar(new RecuperacaoSenhaController.Solicitar("ausente@example.com"),request));
            for(int i=0;i<3;i++) controller.solicitar(new RecuperacaoSenhaController.Solicitar("ausente@example.com"),request);
            assertEquals(429,assertThrows(ResponseStatusException.class, () -> controller.solicitar(new RecuperacaoSenhaController.Solicitar("cliente@example.com"),request)).getStatusCode().value());
        } finally { controller.fechar(); }
    }
    @Test void disabledDeliveryNeverClaimsEmailSent() {
        var controller=new RecuperacaoSenhaController(mock(RecuperacaoSenhaService.class),mock(RecuperacaoEmail.class));
        try { assertEquals(503,assertThrows(ResponseStatusException.class, () -> controller.solicitar(new RecuperacaoSenhaController.Solicitar("cliente@example.com"),new MockHttpServletRequest())).getStatusCode().value()); }
        finally { controller.fechar(); }
    }
}
