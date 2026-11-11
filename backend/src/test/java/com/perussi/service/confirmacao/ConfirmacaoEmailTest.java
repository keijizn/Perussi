package com.perussi.service.confirmacao;

import com.perussi.model.Usuario;
import com.perussi.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql=false, properties="spring.jpa.hibernate.ddl-auto=create-drop")
@Import({ConfirmacaoEmailService.class, ConfirmacaoEmailTest.Config.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class ConfirmacaoEmailTest {
    static class Caixa implements ConfirmacaoEmail {
        String token; int envios; boolean falhar; boolean ativo=true;
        public boolean habilitado() { return ativo; }
        public void enviar(String email, String token) {
            if (falhar) throw new IllegalStateException("simulado");
            this.token=token; envios++;
        }
    }
    @TestConfiguration static class Config { @Bean Caixa caixa() { return new Caixa(); } }
    @Autowired ConfirmacaoEmailService service;
    @Autowired UsuarioRepository usuarios;
    @Autowired Caixa caixa;
    @Autowired PlatformTransactionManager manager;
    Usuario user;
    TransactionTemplate tx;
    @BeforeEach void setup() {
        caixa.token=null; caixa.envios=0; caixa.falhar=false; caixa.ativo=true;
        tx=new TransactionTemplate(manager);
        user=usuarios.saveAndFlush(Usuario.builder().email(UUID.randomUUID()+"@example.com").nome("Teste").senha("hash").role("CUSTOMER").build());
    }
    private Usuario atual() { return usuarios.findById(user.getId()).orElseThrow(); }
    private void editar(java.util.function.Consumer<Usuario> edicao) {
        tx.executeWithoutResult(s -> edicao.accept(usuarios.findLockedById(user.getId()).orElseThrow()));
    }
    @Test void existingAccountStartsUnconfirmedAndTokenIsHashedSingleUse() {
        assertFalse(service.situacao(user.getEmail()).confirmado());
        service.solicitar(user.getId());
        assertEquals(43, caixa.token.length());
        assertEquals(64, atual().getConfirmacaoHash().length());
        assertNotEquals(caixa.token, atual().getConfirmacaoHash());
        service.confirmar(caixa.token);
        assertTrue(atual().getEmailConfirmado());
        assertNull(atual().getConfirmacaoHash());
        assertEquals(user.getId(), atual().getId());
        assertThrows(ResponseStatusException.class, () -> service.confirmar(caixa.token));
        service.solicitar(user.getId());
        assertEquals(1, caixa.envios);
    }
    @Test void expiredOrWrongTokenDoesNotConfirmAccount() {
        service.solicitar(user.getId());
        assertThrows(ResponseStatusException.class, () -> service.confirmar("A".repeat(43)));
        editar(u -> u.setConfirmacaoExpiraEm(Instant.now().minusSeconds(1)));
        assertThrows(ResponseStatusException.class, () -> service.confirmar(caixa.token));
        assertFalse(Boolean.TRUE.equals(atual().getEmailConfirmado()));
    }
    @Test void changedEmailCannotBeConfirmedByOldLink() {
        service.solicitar(user.getId());
        editar(u -> u.setEmail("novo-"+UUID.randomUUID()+"@example.com"));
        assertThrows(ResponseStatusException.class, () -> service.confirmar(caixa.token));
    }
    @Test void resendInvalidatesPreviousLinkAndIsThrottled() {
        service.solicitar(user.getId());
        String anterior=caixa.token;
        assertEquals(429, assertThrows(ResponseStatusException.class, () -> service.solicitar(user.getId())).getStatusCode().value());
        editar(u -> u.setConfirmacaoSolicitadaEm(Instant.now().minusSeconds(61)));
        service.solicitar(user.getId());
        assertNotEquals(anterior, caixa.token);
        assertThrows(ResponseStatusException.class, () -> service.confirmar(anterior));
        service.confirmar(caixa.token);
        assertTrue(atual().getEmailConfirmado());
    }
    @Test void providerFailureKeepsPreviouslySentLinkUsable() {
        service.solicitar(user.getId());
        String token=caixa.token;
        editar(u -> u.setConfirmacaoSolicitadaEm(Instant.now().minusSeconds(61)));
        caixa.falhar=true;
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.solicitar(user.getId())).getStatusCode().value());
        service.confirmar(token);
        assertTrue(atual().getEmailConfirmado());
    }
    @Test void disabledSenderDoesNotCreateToken() {
        caixa.ativo=false;
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.solicitar(user.getId())).getStatusCode().value());
        assertNull(atual().getConfirmacaoHash());
    }
    @Test void limitsFiveMessagesPerWindow() {
        for(int i=0;i<5;i++) {
            editar(u -> u.setConfirmacaoSolicitadaEm(Instant.now().minusSeconds(61)));
            service.solicitar(user.getId());
        }
        editar(u -> u.setConfirmacaoSolicitadaEm(Instant.now().minusSeconds(61)));
        assertEquals(429, assertThrows(ResponseStatusException.class, () -> service.solicitar(user.getId())).getStatusCode().value());
        assertEquals(5, caixa.envios);
    }
    @Test void concurrentConfirmationConsumesTokenOnlyOnce() throws Exception {
        service.solicitar(user.getId());
        String token=caixa.token;
        var pool=Executors.newFixedThreadPool(2);
        var start=new CountDownLatch(1);
        Callable<Boolean> task=() -> { start.await(); try { service.confirmar(token); return true; } catch(ResponseStatusException e) { return false; } };
        try {
            var a=pool.submit(task); var b=pool.submit(task); start.countDown();
            assertEquals(1, (a.get(15,TimeUnit.SECONDS)?1:0)+(b.get(15,TimeUnit.SECONDS)?1:0));
        } finally { pool.shutdownNow(); }
    }
}
