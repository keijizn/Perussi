package com.perussi.service.recuperacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class RecuperacaoEmailJsTest {
    private RecuperacaoEmailJs adapter(RestClient.Builder builder, String chave) {
        return new RecuperacaoEmailJs(builder, true, "http://localhost:5173/", "servico", "modelo", "publica", chave);
    }

    @Test void enviaCamposDoModeloParaApiSemIncluirChaveNoLink() {
        var recebido = new AtomicBoolean();
        var builder = RestClient.builder().requestInterceptor((request, body, execution) -> {
            assertEquals("https://api.emailjs.com/api/v1.0/email/send", request.getURI().toString());
            var json = new ObjectMapper().readTree(body);
            assertEquals("servico", json.path("service_id").asText());
            assertEquals("modelo", json.path("template_id").asText());
            assertEquals("publica", json.path("user_id").asText());
            assertEquals("privada", json.path("accessToken").asText());
            assertEquals("cliente@example.com", json.at("/template_params/email").asText());
            assertEquals("http://localhost:5173/redefinir-senha#token=teste", json.at("/template_params/link").asText());
            assertTrue(json.at("/template_params/confirmacao").isBoolean());
            assertEquals(false, json.at("/template_params/confirmacao").asBoolean());
            recebido.set(true);
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        });
        adapter(builder, "privada").enviar("cliente@example.com", "teste");
        assertTrue(recebido.get());
    }

    @Test void chaveAusenteMantemEnvioIndisponivel() {
        assertFalse(adapter(RestClient.builder(), "").habilitado());
    }

    @Test void recusaDoProvedorSePropagaParaReverterToken() {
        var builder = RestClient.builder().requestInterceptor((req, body, execution) ->
                new MockClientHttpResponse(new byte[0], HttpStatus.FORBIDDEN));
        assertThrows(RestClientResponseException.class, () -> adapter(builder, "privada").enviar("cliente@example.com", "teste"));
    }
}
