package com.perussi.service;

import com.mercadopago.client.payment.*;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;
import com.perussi.model.Pedido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Optional;

@Component
public class MercadoPagoPixGateway implements PixGateway {
    private final String notificationUrl;
    private final MercadoPagoOrdersClient orders;
    private final String creationApi;
    public MercadoPagoPixGateway(@Value("${mercadopago.notification-url:}") String notificationUrl,
            MercadoPagoOrdersClient orders, @Value("${mercadopago.creation-api:payments}") String creationApi) {
        this.notificationUrl = notificationUrl;
        this.orders = orders;
        if (!java.util.Set.of("payments", "orders").contains(creationApi)) throw new IllegalArgumentException("API invalida");
        this.creationApi = creationApi;
    }

    public void preparar(Pedido pedido) {
        if (pedido.getPagamentoApi() == null) {
            pedido.setPagamentoApi(Boolean.TRUE.equals(pedido.getPagamentoSolicitado()) ? "payments" : creationApi);
        }
    }

    public Optional<Pagamento> recuperar(Pedido pedido) {
        // Without a returned Order ID, never create a fresh order while cancelling.
        // Keep the reservation for reconciliation; retrying creation uses the same idempotency key.
        return "orders".equals(pedido.getPagamentoApi()) ? Optional.empty() : recuperar(pedido.getId().toString());
    }

    private MPRequestOptions options(Map<String, String> headers) {
        return MPRequestOptions.builder().connectionTimeout(5000).socketTimeout(10000)
                .customHeaders(headers).build();
    }

    public Pagamento criar(Pedido pedido) {
        if ("orders".equals(pedido.getPagamentoApi())) return orders.criar(pedido);
        try {
            var builder = PaymentCreateRequest.builder().transactionAmount(pedido.getTotal())
                    .description("Pedido Perussi #" + pedido.getId()).paymentMethodId("pix")
                    .externalReference(pedido.getId().toString())
                    .payer(PaymentPayerRequest.builder().email(pedido.getUsuario().getEmail())
                            .firstName(pedido.getUsuario().getNome()).build());
            if (!notificationUrl.isBlank()) builder.notificationUrl(notificationUrl);
            return converter(new PaymentClient().create(builder.build(),
                    options(Map.of("X-Idempotency-Key", pedido.getId().toString()))));
        } catch (Exception exception) { throw indisponivel(exception); }
    }

    public Pagamento consultar(String id) {
        if (id.startsWith("ORD")) return orders.consultar(id);
        try { return converter(new PaymentClient().get(Long.valueOf(id), options(Map.of()))); }
        catch (Exception exception) { throw indisponivel(exception); }
    }

    public Pagamento cancelar(String id) {
        if (id.startsWith("ORD")) return orders.cancelar(id);
        try { return converter(new PaymentClient().cancel(Long.valueOf(id), options(Map.of()))); }
        catch (Exception exception) {
            // A payment may have been approved while the cancellation was in flight.
            return consultar(id);
        }
    }

    public Optional<Pagamento> recuperar(String referencia) {
        try {
            var result = new PaymentClient().search(MPSearchRequest.builder()
                    .filters(Map.of("external_reference", referencia)).limit(2).build(), options(Map.of()));
            if (result.getResults().size() > 1) {
                throw new IllegalStateException("Mais de um pagamento para o pedido");
            }
            return result.getResults().stream().findFirst().map(this::converter);
        } catch (Exception exception) { throw indisponivel(exception); }
    }

    private Pagamento converter(Payment payment) {
        var interaction = payment.getPointOfInteraction();
        var data = interaction == null ? null : interaction.getTransactionData();
        return new Pagamento(String.valueOf(payment.getId()), payment.getStatus(), payment.getTransactionAmount(),
                payment.getExternalReference(), data == null ? null : data.getQrCode(),
                data == null ? null : data.getQrCodeBase64());
    }

    private ResponseStatusException indisponivel(Exception exception) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Não foi possível confirmar o pagamento com o Mercado Pago. Tente novamente.", exception);
    }
}
