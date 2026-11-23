package com.perussi.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.hamcrest.Matchers.startsWith;

class ImagemStorageServiceTest {
    private final byte[] png = {(byte)137,80,78,71,13,10,26,10,0};

    @Test void enviaBytesEDevolveUrlPublicaSemChave() {
        var builder = RestClient.builder();
        var service = new ImagemStorageService(builder, "https://example.supabase.co", "sb_secret_teste", "produtos");
        var mockBuilder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(mockBuilder).build();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "cliente", mockBuilder.build());
        server.expect(requestTo(startsWith("https://example.supabase.co/storage/v1/object/produtos/")))
                .andExpect(header("apikey", "sb_secret_teste"))
                .andExpect(content().bytes(png)).andRespond(withSuccess());
        String result = service.enviar(new MockMultipartFile("file", "../../foto.png", "image/png", png));
        assertTrue(result.matches("https://example.supabase.co/storage/v1/object/public/produtos/[a-f0-9-]+\\.png"));
        assertFalse(result.contains("sb_secret"));
        server.verify();
    }
    @Test void rejeitaArquivoDisfarcadoDeImagem() {
        var service = new ImagemStorageService(RestClient.builder(), "", "", "produtos");
        var ex = assertThrows(ResponseStatusException.class, () -> service.enviar(new MockMultipartFile("file", "foto.png", "image/png", "<script>bad</script>".getBytes())));
        assertEquals(400, ex.getStatusCode().value());
    }
    @Test void rejeitaLimiteEConfigAusente() {
        var service = new ImagemStorageService(RestClient.builder(), "", "", "produtos");
        assertEquals(413, assertThrows(ResponseStatusException.class, () -> service.enviar(new MockMultipartFile("file", "a.png", "image/png", new byte[5*1024*1024+1]))).getStatusCode().value());
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.enviar(new MockMultipartFile("file", "a.png", "image/png", png))).getStatusCode().value());
    }
}
