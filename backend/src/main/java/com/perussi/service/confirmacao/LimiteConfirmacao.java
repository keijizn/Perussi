package com.perussi.service.confirmacao;

import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Component
public class LimiteConfirmacao {
    private record Janela(long inicio, int tentativas) {}
    private final Map<String, Janela> limites = new HashMap<>();
    public synchronized void verificar(String chave, int maximo) {
        long agora = System.currentTimeMillis();
        limites.entrySet().removeIf(e -> agora - e.getValue().inicio() >= 900_000);
        Janela janela = limites.get(chave);
        if ((janela == null && limites.size() >= 10_000) || (janela != null && janela.tentativas() >= maximo))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde 15 minutos.");
        limites.put(chave, new Janela(janela == null ? agora : janela.inicio(), janela == null ? 1 : janela.tentativas() + 1));
    }
}
