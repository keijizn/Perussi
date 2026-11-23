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
        for (boolean alternativa : new boolean[]{false, true}) {
            try {
                String url = alternativa ? "https://brasilapi.com.br/api/cep/v2/" + limpo
                        : "https://viacep.com.br/ws/" + limpo + "/json/";
                var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
                var response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (alternativa && response.statusCode() == 404)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CEP não encontrado.");
                if (response.statusCode() != 200) throw new java.io.IOException("CEP indisponível");
                var data = json.readTree(response.body());
                if (data == null || !data.isObject()) throw new java.io.IOException("Resposta incompleta");
                if (!alternativa && data.path("erro").asBoolean(false))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CEP não encontrado.");
                String cidade = data.path(alternativa ? "city" : "localidade").asText().trim();
                String estado = data.path(alternativa ? "state" : "uf").asText().trim();
                if (cidade.isBlank() || estado.isBlank()) throw new java.io.IOException("Resposta incompleta");
                if (!"Curitiba".equalsIgnoreCase(cidade) || !"PR".equals(estado))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No momento, entregamos somente em Curitiba (PR).");
                return new Entrega(limpo, data.path(alternativa ? "street" : "logradouro").asText(),
                        data.path(alternativa ? "neighborhood" : "bairro").asText(), "Curitiba", "PR", FRETE);
            } catch (ResponseStatusException e) { throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw indisponivel();
            } catch (java.io.IOException | IllegalArgumentException e) {
                // Só falhas técnicas usam a alternativa; CEP fora da área continua bloqueado.
            }
        }
        throw indisponivel();
    }
    private ResponseStatusException indisponivel() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível validar a entrega pelo CEP. Tente novamente.");
    }
}
