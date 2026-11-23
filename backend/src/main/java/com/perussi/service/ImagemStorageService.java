package com.perussi.service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImagemStorageService {
    private final RestClient cliente;
    private final String url, chave, bucket;
    private final boolean configurado;
    static final long LIMITE = 5 * 1024 * 1024;

    public ImagemStorageService(RestClient.Builder builder,
            @Value("${storage.supabase.url:}") String url,
            @Value("${storage.supabase.secret-key:}") String chave,
            @Value("${storage.supabase.bucket:produtos}") String bucket) {
        this.url = url.trim().replaceAll("/+$", "");
        this.chave = chave.trim();
        this.bucket = bucket.trim();
        configurado = !this.url.isBlank() && !this.chave.isBlank();
        if (configurado) {
            URI uri = URI.create(this.url);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null ||
                    uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null ||
                    !uri.getPath().isEmpty() || !this.bucket.matches("[a-z0-9][a-z0-9_-]*")) {
                throw new IllegalArgumentException("Configuracao invalida do armazenamento de imagens.");
            }
        }
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(20000);
        cliente = builder.requestFactory(factory).build();
    }

    public String enviar(MultipartFile arquivo) {
        if (arquivo.isEmpty()) throw erro(HttpStatus.BAD_REQUEST, "Escolha uma imagem.");
        if (arquivo.getSize() > LIMITE) throw erro(HttpStatus.PAYLOAD_TOO_LARGE, "A imagem deve ter no maximo 5 MB.");
        byte[] bytes;
        try { bytes = arquivo.getBytes(); }
        catch (IOException ex) { throw erro(HttpStatus.BAD_REQUEST, "Nao foi possivel ler a imagem."); }
        String tipo = tipo(bytes);
        if (tipo == null || !tipo.equals(arquivo.getContentType()))
            throw erro(HttpStatus.BAD_REQUEST, "Envie uma imagem JPG, PNG ou WebP valida.");
        if (!configurado) throw erro(HttpStatus.SERVICE_UNAVAILABLE, "Envio de imagens ainda nao configurado.");
        String extensao = tipo.equals("image/jpeg") ? "jpg" : tipo.equals("image/png") ? "png" : "webp";
        String caminho = bucket + "/" + UUID.randomUUID() + "." + extensao;
        try {
            cliente.post().uri(url + "/storage/v1/object/" + caminho)
                    .header("apikey", chave).header("x-upsert", "false")
                    .contentType(MediaType.parseMediaType(tipo)).body(bytes)
                    .retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            throw erro(HttpStatus.BAD_GATEWAY, "Nao foi possivel enviar a imagem. Tente novamente.");
        }
        return url + "/storage/v1/object/public/" + caminho;
    }

    private static String tipo(byte[] b) {
        if (b.length >= 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255) return "image/jpeg";
        byte[] png = {(byte)137,80,78,71,13,10,26,10};
        if (b.length >= 8 && java.util.Arrays.equals(java.util.Arrays.copyOf(b,8),png)) return "image/png";
        if (b.length >= 12 && new String(b,0,4,StandardCharsets.US_ASCII).equals("RIFF") &&
                new String(b,8,4,StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
        return null;
    }

    private static ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
