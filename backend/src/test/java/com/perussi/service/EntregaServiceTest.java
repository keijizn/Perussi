package com.perussi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class EntregaServiceTest {
    @Test void fallbackOnTimeoutPreservesCuritibaRule() throws Exception {
        @SuppressWarnings("unchecked") HttpResponse<String> result = mock(HttpResponse.class);
        when(result.statusCode()).thenReturn(200);
        when(result.body()).thenReturn("{\"city\":\"Curitiba\",\"state\":\"PR\",\"street\":\"Rua José Loureiro\",\"neighborhood\":\"Centro\"}");
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new java.io.IOException("timeout")).thenReturn(result);
        var entrega = service.consultar("80010-000");
        assertEquals("80010000", entrega.cep());
        assertEquals("Rua José Loureiro", entrega.logradouro());
        assertEquals(new java.math.BigDecimal("15.00"), entrega.frete());
        var requests = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(http, times(2)).send(requests.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertEquals("brasilapi.com.br", requests.getAllValues().get(1).uri().getHost());
    }

    @Test void fallbackRejectsOutsideCuritiba() throws Exception {
        @SuppressWarnings("unchecked") HttpResponse<String> result = mock(HttpResponse.class);
        when(result.statusCode()).thenReturn(200);
        when(result.body()).thenReturn("{\"city\":\"Colombo\",\"state\":\"PR\"}");
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new java.io.IOException("timeout")).thenReturn(result);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.consultar("83400000")).getStatusCode().value());
    }
    private final HttpClient http = mock(HttpClient.class);
    private final EntregaService service = new EntregaService(new ObjectMapper(), http);

    private void response(String body) throws Exception {
        @SuppressWarnings("unchecked") HttpResponse<String> result = mock(HttpResponse.class);
        when(result.statusCode()).thenReturn(200);
        when(result.body()).thenReturn(body);
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(result);
    }

    @Test void curitibaGetsFixedFreightAndNormalizedCep() throws Exception {
        response("{\"localidade\":\"Curitiba\",\"uf\":\"PR\",\"logradouro\":\"Rua\"}");
        var entrega = service.consultar("80020-000");
        assertEquals("80020000", entrega.cep());
        assertEquals(new java.math.BigDecimal("15.00"), entrega.frete());
        assertEquals("Curitiba", entrega.cidade());
    }

    @Test void rejectsOtherCitiesIncludingMetropolitanArea() throws Exception {
        for (String cidade : new String[]{"São José dos Pinhais", "Colombo", "São Paulo"}) {
            response("{\"localidade\":\"" + cidade + "\",\"uf\":\"PR\"}");
            assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.consultar("83005000")).getStatusCode().value());
        }
    }

    @Test void invalidCepNeverCallsProvider() {
        for (String cep : new String[]{"", "123", "abcdefgh", "800200000", "80020/00"})
            assertThrows(ResponseStatusException.class, () -> service.consultar(cep));
        verifyNoInteractions(http);
    }

    @Test void unknownCepIsRejected() throws Exception {
        response("{\"erro\":true}");
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.consultar("99999999")).getStatusCode().value());
    }

    @Test void unavailableOrMalformedProviderNeverAllowsCheckout() throws Exception {
        response("{}");
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.consultar("80020000")).getStatusCode().value());
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(new java.io.IOException("timeout"));
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.consultar("80020000")).getStatusCode().value());
    }
}
