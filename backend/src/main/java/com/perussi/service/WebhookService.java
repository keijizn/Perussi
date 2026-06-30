package com.perussi.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.perussi.model.Pedido;
import com.perussi.model.StatusPedido;
import com.perussi.repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebhookService {

    private final PedidoRepository pedidoRepository;

    public WebhookService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public void processarWebhook(Map<String, Object> payload) {

        try {

            System.out.println("WEBHOOK RECEBIDO:");
            System.out.println(payload);

            Object dataObj = payload.get("data");

            if (!(dataObj instanceof Map<?, ?> data)) {
                return;
            }

            Object paymentIdObj = data.get("id");

            if (paymentIdObj == null) {
                return;
            }

            String paymentId = String.valueOf(paymentIdObj);

            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.valueOf(paymentId));

            Pedido pedido = pedidoRepository
                    .findByMercadoPagoPaymentId(paymentId)
                    .orElse(null);

            if (pedido == null) {
                return;
            }

            pedido.setMercadoPagoStatus(payment.getStatus());

            if ("approved".equalsIgnoreCase(payment.getStatus())) {
                pedido.setStatus(StatusPedido.PAGO);
            }

            pedidoRepository.save(pedido);

            System.out.println("Pedido atualizado:");
            System.out.println("Pedido: " + pedido.getId());
            System.out.println("Status MP: " + payment.getStatus());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}