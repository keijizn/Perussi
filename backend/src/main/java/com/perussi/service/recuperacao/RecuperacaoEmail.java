package com.perussi.service.recuperacao;

public interface RecuperacaoEmail {
    boolean habilitado();
    void enviar(String email, String token);
}
