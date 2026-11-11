package com.perussi.service.confirmacao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.util.Map;

@Service
public class ConfirmacaoEmailJs implements ConfirmacaoEmail {
    private final RestClient cliente;
    private final boolean ativo;
    private final String site, servico, modelo, publica, privada;

    public ConfirmacaoEmailJs(RestClient.Builder builder,
            @Value("${confirmacao.habilitada:false}") boolean ativo,
            @Value("${recuperacao.url-site:}") String site,
            @Value("${emailjs.service-id:}") String servico,
            @Value("${emailjs.template-id:}") String modelo,
            @Value("${emailjs.public-key:}") String publica,
            @Value("${emailjs.private-key:}") String privada) {
        this.ativo = ativo;
        this.site = site.replaceAll("/+$", "");
        this.servico = servico.trim();
        this.modelo = modelo.trim();
        this.publica = publica.trim();
        this.privada = privada.trim();
        if (ativo) {
            URI url = URI.create(this.site);
            boolean local = "http".equals(url.getScheme()) && "localhost".equals(url.getHost());
            if (url.getHost() == null || url.getUserInfo() != null || url.getQuery() != null ||
                    url.getFragment() != null || !(local || "https".equals(url.getScheme()))) {
                throw new IllegalArgumentException("Configure a URL do site para confirmação de email.");
            }
        }
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        cliente = builder.requestFactory(factory).baseUrl("https://api.emailjs.com").build();
    }

    public boolean habilitado() {
        return ativo && !servico.isBlank() && !modelo.isBlank() && !publica.isBlank() && !privada.isBlank();
    }

    public void enviar(String email, String token) {
        if (!habilitado()) throw new IllegalStateException("Envio pelo EmailJS indisponível.");
        var parametros = Map.of("email", email, "link", site + "/confirmar-email#token=" + token,
                "confirmacao", true);
        var corpo = Map.of("service_id", servico, "template_id", modelo, "user_id", publica,
                "accessToken", privada, "template_params", parametros);
        cliente.post().uri("/api/v1.0/email/send").contentType(MediaType.APPLICATION_JSON)
                .body(corpo).retrieve().toBodilessEntity();
    }
}
