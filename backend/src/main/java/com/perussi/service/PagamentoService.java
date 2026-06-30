package com.perussi.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;
import com.perussi.dto.CriarPagamentoDTO;
import com.perussi.dto.PagamentoResponseDTO;
import com.perussi.model.Pedido;
import com.perussi.model.StatusPedido;
import com.perussi.repository.PedidoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PagamentoService {

        private final PedidoRepository pedidoRepository;

        public PagamentoService(PedidoRepository pedidoRepository) {
                this.pedidoRepository = pedidoRepository;
        }

        public PagamentoResponseDTO criarPagamento(CriarPagamentoDTO dto) {
                Pedido pedido = pedidoRepository.findById(dto.pedidoId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Pedido não encontrado"));

                if (pedido.getStatus() != StatusPedido.PENDENTE) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Apenas pedidos pendentes podem gerar pagamento");
                }

                if (!"PIX".equalsIgnoreCase(dto.metodoPagamento())) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Nesta etapa, apenas PIX está habilitado no Mercado Pago");
                }

                try {
                        PaymentClient client = new PaymentClient();

                        PaymentCreateRequest request = PaymentCreateRequest.builder()
                                        .transactionAmount(pedido.getTotal())
                                        .description("Pedido Perussi #" + pedido.getId())
                                        .paymentMethodId("pix")
                                        .payer(
                                                        PaymentPayerRequest.builder()
                                                                        .email(pedido.getUsuario().getEmail())
                                                                        .firstName(pedido.getUsuario().getNome())
                                                                        .build())
                                        .build();

                        Payment payment = client.create(request);

                        String qrCode = payment
                                        .getPointOfInteraction()
                                        .getTransactionData()
                                        .getQrCode();

                        String qrCodeBase64 = payment
                                        .getPointOfInteraction()
                                        .getTransactionData()
                                        .getQrCodeBase64();

                        pedido.setMercadoPagoPaymentId(String.valueOf(payment.getId()));
                        pedido.setMercadoPagoStatus(payment.getStatus());

                        pedidoRepository.save(pedido);

                        return new PagamentoResponseDTO(
                                        String.valueOf(payment.getId()),
                                        payment.getStatus(),
                                        qrCode,
                                        qrCodeBase64);
                } catch (Exception e) {
                        e.printStackTrace();

                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Erro ao criar pagamento no Mercado Pago: " + e.getMessage());
                }
        }
}