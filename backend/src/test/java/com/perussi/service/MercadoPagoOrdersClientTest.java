package com.perussi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.perussi.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.net.http.*;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MercadoPagoOrdersClientTest {
    private final ObjectMapper json = new ObjectMapper();
    private String response(String status, String detail) {
        return """
          {"id":"ORD01TEST", "total_amount":"50.00", "external_reference":"ref",
           "status":"%s", "status_detail":"%s", "transactions":{"payments":[{
           "amount":"50.00", "status":"%s", "status_detail":"%s",
           "payment_method":{"id":"pix", "qr_code":"copy-code", "qr_code_base64":""}}]}}
          """.formatted(status, detail, status, detail);
    }

    @Test void onlyAccreditedPaymentsBecomeApproved() throws Exception {
        assertEquals("approved", MercadoPagoOrdersClient.converter(json.readTree(response("processed", "accredited"))).status());
        for (String detail : new String[]{"waiting_transfer", "partially_refunded", "unknown"})
            assertEquals("pending", MercadoPagoOrdersClient.converter(json.readTree(response("processed", detail))).status());
        assertEquals("cancelled", MercadoPagoOrdersClient.converter(json.readTree(response("canceled", "canceled"))).status());
        assertEquals("cancelled", MercadoPagoOrdersClient.converter(json.readTree(response("expired", "expired"))).status());
        assertEquals("rejected", MercadoPagoOrdersClient.converter(json.readTree(response("failed", "failed"))).status());
    }

    @Test void rejectsAmountMismatchAndMultipleTransactions() throws Exception {
        var node = json.readTree(response("processed", "accredited"));
        ((com.fasterxml.jackson.databind.node.ObjectNode)node).put("total_amount", "51.00");
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> MercadoPagoOrdersClient.converter(node));
        var multiple = json.readTree(response("processed", "accredited"));
        ((com.fasterxml.jackson.databind.node.ArrayNode)multiple.path("transactions").path("payments")).add(multiple.path("transactions").path("payments").get(0).deepCopy());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> MercadoPagoOrdersClient.converter(multiple));
    }

    @Test void retryUsesSameIdempotencyKeyAndReturnsCopyCodeWithoutImage() throws Exception {
        HttpClient http = mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<String> remote = mock(HttpResponse.class);
        when(remote.statusCode()).thenReturn(201);
        when(remote.body()).thenReturn(response("action_required", "waiting_transfer"));
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(remote);
        var client = new MercadoPagoOrdersClient("test-only", json, http);
        var pedido = Pedido.builder().id(UUID.randomUUID()).total(new BigDecimal("50.00"))
                .usuario(Usuario.builder().email("test_user_br@testuser.com").nome("APRO").build()).build();
        assertEquals("copy-code", client.criar(pedido).qrCode());
        client.criar(pedido);
        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http, times(2)).send(requests.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        for (var request : requests.getAllValues()) {
            assertEquals(pedido.getId().toString(), request.headers().firstValue("X-Idempotency-Key").orElseThrow());
            assertEquals("https://api.mercadopago.com/v1/orders", request.uri().toString());
        }
    }

    @Test void cancelledRequestFailureStillQueriesCurrentStatus() throws Exception {
        HttpClient http = mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<String> remote = mock(HttpResponse.class);
        when(remote.statusCode()).thenReturn(200);
        when(remote.body()).thenReturn(response("processed", "accredited"));
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new java.io.IOException("timeout")).thenReturn(remote);
        assertEquals("approved", new MercadoPagoOrdersClient("test-only", json, http).cancelar("ORD01TEST").status());
    }

    @Test void migrationKeepsUncertainLegacyIntentsOnLegacyApi() {
        var router = new MercadoPagoPixGateway("", mock(MercadoPagoOrdersClient.class), "orders");
        var old = Pedido.builder().pagamentoSolicitado(true).build();
        router.preparar(old);
        assertEquals("payments", old.getPagamentoApi());
        var fresh = Pedido.builder().id(UUID.randomUUID()).build();
        router.preparar(fresh);
        assertEquals("orders", fresh.getPagamentoApi());
        assertTrue(router.recuperar(fresh).isEmpty());
    }
}
