package com.perussi.service.recuperacao;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.net.URI;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "recuperacao.provedor", havingValue = "smtp", matchIfMissing = true)
public class RecuperacaoEmailSmtp implements RecuperacaoEmail {
    private final JavaMailSender sender;
    private final String remetente;
    private final String baseUrl;
    private final boolean ativo;

    public RecuperacaoEmailSmtp(ObjectProvider<JavaMailSender> provider,
            @Value("${recuperacao.habilitada:false}") boolean ativo,
            @Value("${recuperacao.remetente:}") String remetente,
            @Value("${recuperacao.url-site:}") String baseUrl) {
        this.sender = provider.getIfAvailable();
        this.remetente = remetente;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.ativo = ativo;
        if (ativo) {
            URI uri = URI.create(this.baseUrl);
            if (sender == null || remetente.isBlank() || remetente.contains("\n") || remetente.contains("\r")
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && "localhost".equals(uri.getHost()))))
                throw new IllegalArgumentException("Configure SMTP, remetente e URL HTTPS do site para recuperação.");
        }
    }

    public boolean habilitado() { return ativo; }

    public void enviar(String email, String token) {
        if (!ativo) throw new IllegalStateException("Recuperação desabilitada");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remetente);
        message.setTo(email);
        message.setSubject("Perussi — redefinir sua senha");
        message.setText("Recebemos uma solicitação para redefinir sua senha.\n\n"
                + "Abra este link em até 15 minutos:\n" + baseUrl + "/redefinir-senha#token=" + token
                + "\n\nO link só pode ser usado uma vez. Se não foi você, ignore este email. Sua senha não foi alterada.");
        sender.send(message);
    }
}
