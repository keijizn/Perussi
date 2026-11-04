package com.perussi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class EntregaServiceTest {
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
