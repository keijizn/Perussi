package com.perussi.controller;

import com.perussi.config.SecurityConfig;
import com.perussi.security.*;
import com.perussi.service.PedidoService;
import com.perussi.service.PagamentoService;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;

@WebMvcTest({PedidoController.class, PagamentoController.class, WebhookController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, WebhookSignatureVerifier.class})
@org.springframework.test.context.TestPropertySource(properties = "mercadopago.webhook-secret=test-secret")
class PaymentSecurityTest {
    private static String signed() throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("test-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        return "ts=1704908010,v1=" + java.util.HexFormat.of().formatHex(mac.doFinal(
                "id:123;request-id:req-1;ts:1704908010;".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void unsignedNotificationNeverReachesService() throws Exception {
        mvc.perform(post("/webhooks/mercadopago").contentType("application/json")
                .content("{\"data\":{\"id\":\"123\"}}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(webhook);
    }

    @Test
    void tamperedNotificationNeverReachesService() throws Exception {
        for (String bodyId : new String[]{"123", "999"}) {
            mvc.perform(post("/webhooks/mercadopago").param("data.id", "123")
                    .header("x-request-id", bodyId.equals("123") ? "changed" : "req-1")
                    .header("x-signature", signed()).contentType("application/json")
                    .content("{\"data\":{\"id\":\"" + bodyId + "\"}}"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(webhook);
    }

    @Test
    void missingConfigurationFailsClosed() {
        var verifier = new WebhookSignatureVerifier("");
        var error = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> verifier.verify(null, null, null, java.util.Map.of()));
        org.junit.jupiter.api.Assertions.assertEquals(503, error.getStatusCode().value());
    }

    @Test
    void signedNotificationIsAccepted() throws Exception {
        mvc.perform(post("/webhooks/mercadopago").param("data.id", "123")
                .header("x-request-id", "req-1").header("x-signature", signed())
                .contentType("application/json").content("{\"data\":{\"id\":\"123\"}}"))
                .andExpect(status().isOk());
        verify(webhook).processarWebhook(anyMap());
    }
    @Autowired MockMvc mvc;
    @MockBean PedidoService pedidos;
    @MockBean PagamentoService pagamentos;
    @MockBean JwtService jwt;
    @MockBean SecurityUserDetailsService users;
    @MockBean com.perussi.service.WebhookService webhook;

    @Test
    void webhookFailureIsNotAcknowledgedAsSuccess() throws Exception {
        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_GATEWAY, "Provedor indisponível"))
                .when(webhook).processarWebhook(anyMap());
        mvc.perform(post("/webhooks/mercadopago").param("data.id", "123").header("x-request-id", "req-1").header("x-signature", signed()).contentType("application/json")
                .content("{\"type\":\"payment\",\"data\":{\"id\":\"123\"}}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    @WithMockUser(username = "cliente@example.com", authorities = "CUSTOMER")
    void customerCannotConfirmPaymentDirectly() throws Exception {
        mvc.perform(put("/pedidos/" + UUID.randomUUID() + "/pagamento")
                .contentType("application/json").content("{\"metodoPagamento\":\"PIX\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(pedidos, pagamentos);
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER")
    void customerCanValidateDeliveryWithoutAdminAccess() throws Exception {
        when(pedidos.consultarEntrega("80020000")).thenReturn(new com.perussi.service.EntregaService.Entrega("80020000", "Rua", "Centro", "Curitiba", "PR", new java.math.BigDecimal("15.00")));
        mvc.perform(get("/pedidos/entrega").param("cep", "80020000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.frete").value(15));
    }

    @Test
    void anonymousCannotCreatePayment() throws Exception {
        mvc.perform(post("/pagamentos/criar").contentType("application/json")
                .content("{\"pedidoId\":\"" + UUID.randomUUID() + "\",\"metodoPagamento\":\"PIX\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(pagamentos);
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER")
    void customerCannotUseAdminStatusRouteToMarkOrderPaid() throws Exception {
        mvc.perform(put("/pedidos/" + UUID.randomUUID() + "/status")
                .contentType("application/json").content("{\"status\":\"PAGO\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(pedidos);
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        when(jwt.validateToken("invalid")).thenThrow(new JWTVerificationException("invalid"));
        mvc.perform(post("/pagamentos/criar").header("Authorization", "Bearer invalid")
                .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(pagamentos, users);
    }

    @Test
    @WithMockUser(username = "cliente@example.com", authorities = "CUSTOMER")
    void passesAuthenticatedIdentityToPaymentService() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/pagamentos/criar").contentType("application/json")
                .content("{\"pedidoId\":\"" + id + "\",\"metodoPagamento\":\"PIX\"}"))
                .andExpect(status().isOk());
        verify(pagamentos).criarPagamento(eq("cliente@example.com"),
                eq(new com.perussi.dto.CriarPagamentoDTO(id, "PIX")));
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER")
    void rejectsMissingPaymentFields() throws Exception {
        mvc.perform(post("/pagamentos/criar").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(pagamentos);
    }
}
