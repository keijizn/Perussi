package com.perussi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.perussi.model.Pedido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.math.BigDecimal;

@Component
public class MercadoPagoOrdersClient {
    private final String token;
    private final ObjectMapper json;
    private final HttpClient http;

    @org.springframework.beans.factory.annotation.Autowired
    public MercadoPagoOrdersClient(@Value("${mercadopago.orders-access-token:}") String token, ObjectMapper json) {
        this(token, json, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    MercadoPagoOrdersClient(String token, ObjectMapper json, HttpClient http) {
        this.token = token;
        this.json = json;
        this.http = http;
    }

    public PixGateway.Pagamento criar(Pedido pedido) {
        String amount = pedido.getTotal().setScale(2).toPlainString();
        var body = Map.of("type", "online", "processing_mode", "automatic",
                "external_reference", pedido.getId().toString(), "total_amount", amount,
                "payer", Map.of("email", pedido.getUsuario().getEmail(), "first_name", pedido.getUsuario().getNome()),
                "transactions", Map.of("payments", List.of(Map.of("amount", amount,
                        "payment_method", Map.of("id", "pix", "type", "bank_transfer")))));
        return converter(request("POST", "/v1/orders", body, pedido.getId().toString()));
    }

    public PixGateway.Pagamento consultar(String id) {
        validarId(id);
        return converter(request("GET", "/v1/orders/" + id, null, null));
    }

    public PixGateway.Pagamento cancelar(String id) {
        validarId(id);
        try {
            request("POST", "/v1/orders/" + id + "/cancel", Map.of(), "cancel-" + id);
        } catch (ResponseStatusException ignored) {
            // On timeout or approval racing with cancellation, query the authoritative state.
        }
        return consultar(id);
    }

    private void validarId(String id) {
        if (id == null || !id.matches("ORD[A-Z0-9]+")) throw new IllegalArgumentException("Order ID invalido");
    }

    private JsonNode request(String method, String path, Object body, String key) {
        if (token.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Configure a credencial da API Orders");
        try {
            var builder = HttpRequest.newBuilder(URI.create("https://api.mercadopago.com" + path))
                    .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json");
            if (key != null) builder.header("X-Idempotency-Key", key);
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw unavailable();
            return json.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (java.io.IOException e) { throw unavailable(); }
    }

    static PixGateway.Pagamento converter(JsonNode order) {
        String id = order.path("id").asText();
        var payments = order.path("transactions").path("payments");
        if (!id.matches("ORD[A-Z0-9]+") || !payments.isArray() || payments.size() != 1)
            throw unavailable();
        var payment = payments.get(0);
        var method = payment.path("payment_method");
        if (!"pix".equals(method.path("id").asText())) throw unavailable();
        try {
            BigDecimal total = new BigDecimal(order.path("total_amount").asText());
            if (total.compareTo(new BigDecimal(payment.path("amount").asText())) != 0) throw unavailable();
            String state = order.path("status").asText();
            String status = "pending";
            if ("processed".equals(state) && "accredited".equals(order.path("status_detail").asText())
                    && "processed".equals(payment.path("status").asText())
                    && "accredited".equals(payment.path("status_detail").asText())) status = "approved";
            else if (Set.of("canceled", "expired").contains(state)) status = "cancelled";
            else if ("failed".equals(state)) status = "rejected";
            return new PixGateway.Pagamento(id, status, total, order.path("external_reference").asText(),
                    method.path("qr_code").asText(null), method.path("qr_code_base64").asText(null));
        } catch (NumberFormatException e) { throw unavailable(); }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Nao foi possivel confirmar a order com o Mercado Pago. Tente novamente.");
    }
}
