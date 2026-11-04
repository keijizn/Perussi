package com.perussi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

@Service
public class EntregaService {
    public static final BigDecimal FRETE = new BigDecimal("15.00");
    private final HttpClient http;
    private final ObjectMapper json;

    public record Entrega(String cep, String logradouro, String bairro, String cidade, String estado, BigDecimal frete) {}

    @org.springframework.beans.factory.annotation.Autowired
    public EntregaService(ObjectMapper json) {
        this(json, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    }

    EntregaService(ObjectMapper json, HttpClient http) { this.json = json; this.http = http; }

    public Entrega consultar(String cep) {
        if (cep == null || !cep.trim().matches("[0-9]{5}-?[0-9]{3}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um CEP válido com 8 dígitos.");
        String limpo = cep.trim().replace("-", "");
        try {
            var request = HttpRequest.newBuilder(URI.create("https://viacep.com.br/ws/" + limpo + "/json/"))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new java.io.IOException("CEP indisponível");
            var data = json.readTree(response.body());
            if (data.path("erro").asBoolean(false))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CEP não encontrado.");
            if (data.path("localidade").asText().isBlank() || data.path("uf").asText().isBlank())
                throw new java.io.IOException("Resposta de CEP incompleta");
            if (!"Curitiba".equalsIgnoreCase(data.path("localidade").asText()) || !"PR".equals(data.path("uf").asText()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No momento, entregamos somente em Curitiba (PR).");
            return new Entrega(limpo, data.path("logradouro").asText(), data.path("bairro").asText(), "Curitiba", "PR", FRETE);
        } catch (ResponseStatusException e) { throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw indisponivel();
        } catch (java.io.IOException | IllegalArgumentException e) { throw indisponivel(); }
    }

    private ResponseStatusException indisponivel() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível validar a entrega pelo CEP. Tente novamente.");
    }
}
