package com.perussi.service.confirmacao;

public interface ConfirmacaoEmail {
    boolean habilitado();
    void enviar(String destinatario, String token);
}
