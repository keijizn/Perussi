package com.perussi.service.confirmacao;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.concurrent.*;

@Component
public class ConfirmacaoEmailListener {
    private final ConfirmacaoEmailService service;
    private final ThreadPoolExecutor fila = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(50));
    public ConfirmacaoEmailListener(ConfirmacaoEmailService service) { this.service = service; }
    @TransactionalEventListener
    public void aposSalvar(ConfirmacaoEmailSolicitada evento) {
        try {
            fila.execute(() -> {
                try { service.solicitar(evento.usuarioId()); }
                catch (Exception e) { registrarFalha(); }
            });
        } catch (RejectedExecutionException e) { registrarFalha(); }
    }
    private void registrarFalha() {
        org.slf4j.LoggerFactory.getLogger(getClass()).warn("Confirmação não enviada automaticamente. O usuário pode solicitar reenvio pelo perfil.");
    }
    @PreDestroy public void fechar() { fila.shutdown(); }
}
