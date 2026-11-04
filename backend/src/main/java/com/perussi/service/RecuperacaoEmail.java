package com.perussi.service;

public interface RecuperacaoEmail {
    boolean habilitado();
    void enviar(String email, String token);
}
