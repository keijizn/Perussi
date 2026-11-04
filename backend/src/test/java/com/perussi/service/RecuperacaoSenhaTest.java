package com.perussi.service;

import com.perussi.model.Usuario;
import com.perussi.repository.UsuarioRepository;
import com.perussi.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql=false, properties="spring.jpa.hibernate.ddl-auto=create-drop")
@Import({RecuperacaoSenhaService.class, RecuperacaoSenhaTest.Config.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class RecuperacaoSenhaTest {
    static class Caixa implements RecuperacaoEmail {
        String token; int envios; boolean falhar;
        public boolean habilitado() { return true; }
        public void enviar(String email, String token) {
            if (falhar) throw new IllegalStateException("Falha simulada");
            this.token=token; envios++;
        }
    }
    @TestConfiguration static class Config {
        @Bean Caixa caixa() { return new Caixa(); }
        @Bean BCryptPasswordEncoder encoder() { return new BCryptPasswordEncoder(4); }
    }
    @Autowired RecuperacaoSenhaService service;
    @Autowired UsuarioRepository usuarios;
    @Autowired Caixa caixa;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired PlatformTransactionManager manager;
    Usuario user;
    TransactionTemplate tx;
    @BeforeEach void setup() {
        caixa.token=null; caixa.envios=0; caixa.falhar=false;
        tx=new TransactionTemplate(manager);
        user=usuarios.saveAndFlush(Usuario.builder().email(UUID.randomUUID()+"@example.com").nome("Teste")
                .senha(encoder.encode("antiga123")).role("CUSTOMER").build());
    }
    private Usuario atual() { return usuarios.findById(user.getId()).orElseThrow(); }
    private void editar(java.util.function.Consumer<Usuario> edicao) {
        tx.executeWithoutResult(s -> edicao.accept(usuarios.findLockedById(user.getId()).orElseThrow()));
    }
    @Test void hashesTokenAndRevokesSessionsAfterSingleUse() {
        JwtService jwt=new JwtService("test-secret-with-at-least-thirty-two-bytes");
        String sessao=jwt.generateToken(user);
        service.solicitar(user.getEmail());
        String token=caixa.token;
        assertNotEquals(token, atual().getRecuperacaoHash());
        assertEquals(RecuperacaoSenhaService.hash(token), atual().getRecuperacaoHash());
        service.redefinir(token,"nova123");
        assertTrue(encoder.matches("nova123",atual().getSenha()));
        assertFalse(encoder.matches("antiga123",atual().getSenha()));
        assertNull(atual().getRecuperacaoHash());
        assertThrows(org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> new SecurityUserDetailsService(usuarios).loadSession(jwt.validateToken(sessao),sessao));
        assertThrows(ResponseStatusException.class, () -> service.redefinir(token,"outra123"));
    }
    @Test void unknownAccountAndCooldownDoNotSendExtraEmails() {
        service.solicitar("inexistente@example.com"); assertEquals(0,caixa.envios);
        service.solicitar(user.getEmail()); service.solicitar(user.getEmail()); assertEquals(1,caixa.envios);
    }
    @Test void expiredAndMalformedLinksCannotChangePassword() {
        service.solicitar(user.getEmail());
        editar(u -> u.setRecuperacaoExpiraEm(Instant.now().minusSeconds(1)));
        assertThrows(ResponseStatusException.class, () -> service.redefinir(caixa.token,"nova123"));
        assertThrows(ResponseStatusException.class, () -> service.redefinir("invalido","nova123"));
        assertTrue(encoder.matches("antiga123",atual().getSenha()));
    }
    @Test void accountAccessChangeInvalidatesOutstandingLink() {
        service.solicitar(user.getEmail());
        editar(u -> u.setVersaoSessao(UUID.randomUUID().toString()));
        assertThrows(ResponseStatusException.class, () -> service.redefinir(caixa.token,"nova123"));
    }
    @Test void weakOrOversizedPasswordDoesNotConsumeLink() {
        service.solicitar(user.getEmail());
        for (String senha: List.of("123","      ","é".repeat(40)))
            assertThrows(ResponseStatusException.class, () -> service.redefinir(caixa.token,senha));
        service.redefinir(caixa.token,"valida123");
    }
    @Test void emailFailureRollsBackTokenAndCooldown() {
        caixa.falhar=true;
        assertThrows(IllegalStateException.class, () -> service.solicitar(user.getEmail()));
        assertNull(atual().getRecuperacaoHash()); assertNull(atual().getRecuperacaoSolicitadaEm());
    }
    @Test void requestingNewLinkInvalidatesPreviousLink() {
        service.solicitar(user.getEmail()); String antigo=caixa.token;
        editar(u -> u.setRecuperacaoSolicitadaEm(Instant.now().minusSeconds(61)));
        service.solicitar(user.getEmail());
        assertThrows(ResponseStatusException.class, () -> service.redefinir(antigo,"nova123"));
        service.redefinir(caixa.token,"nova123");
    }
    @Test void concurrentResetsConsumeTokenOnlyOnce() throws Exception {
        service.solicitar(user.getEmail()); String token=caixa.token;
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        Callable<Boolean> reset=() -> {
            start.await();
            try { service.redefinir(token,"nova123"); return true; }
            catch (ResponseStatusException expected) { return false; }
        };
        try {
            var a=pool.submit(reset); var b=pool.submit(reset); start.countDown();
            assertEquals(1,(a.get(15,TimeUnit.SECONDS)?1:0)+(b.get(15,TimeUnit.SECONDS)?1:0));
        } finally { pool.shutdownNow(); }
    }
}
