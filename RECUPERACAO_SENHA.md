# Recuperação de senha

Implementação de 29/10/2026. O envio está **desativado por padrão**. A interface está em `/esqueci-senha` e `/redefinir-senha`; o login contém o atalho. Não há rota pública que revele o link ou o token.

## Funcionamento

- A solicitação responde igualmente para email cadastrado e desconhecido. O envio acontece em uma fila limitada, sem bloquear a resposta com a consulta da conta.
- O link tem validade de 15 minutos, 32 bytes aleatórios e uso único. Apenas o hash SHA-256 fica no banco; o token original vai no fragmento do link por email. A tela remove o fragmento do endereço ao abrir. Recarregar a tela exige abrir novamente o link original.
- Nova solicitação permitida após 60 segundos por conta; um novo link invalida o anterior. Links anteriores também perdem validade quando email ou senha são alterados no perfil.
- A troca é protegida por bloqueio da conta no banco. Duas tentativas simultâneas não podem reutilizar o mesmo link.
- Redefinir a senha encerra as sessões antigas. O usuário precisa entrar novamente.
- Limites por IP: cinco solicitações e vinte redefinições por quinze minutos, em memória. Antes de usar várias instâncias, adotar armazenamento compartilhado. Atrás de proxy, configurar encaminhamento confiável; não confiar livremente em cabeçalhos fornecidos pelo cliente. Sem isso, clientes do mesmo proxy dividem o limite.
- Falhas de envio não imprimem token, senha, destinatário ou exceção SMTP. O token é revertido quando o envio falha. Se a mensagem chegar mas a confirmação do servidor falhar, será necessário solicitar outro link.

## Email da loja

A loja escolheu o EmailJS com o serviço Outlook para enviar o modelo Password Reset. O backend usa a API de envio com os parâmetros `email` e `link`. No perfil local de testes, os IDs e as chaves ficam em `backend/emailjs.local.properties`, ignorado pelo Git. Preencha a chave privada apenas nesse arquivo. Reinicie o backend atualizado após salvar. A entrega real ainda precisa ser confirmada. Para produção, configure o provedor, as chaves e a URL HTTPS da loja no ambiente do backend.

Referência: https://support.microsoft.com/en-us/outlook/pop-imap-and-smtp-settings-for-outlook-com

## Configuração de um provedor SMTP compatível

Somente depois de configurar e validar o provedor, definir no ambiente do backend:

```text
RECUPERACAO_HABILITADA=true
RECUPERACAO_REMETENTE=<remetente autorizado pelo provedor>
RECUPERACAO_URL_SITE=https://<dominio da loja>
SPRING_MAIL_HOST=<servidor SMTP>
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=<usuario SMTP>
SPRING_MAIL_PASSWORD=<credencial SMTP do provedor, nunca a senha pessoal>
```

O transporte exige STARTTLS e validação do certificado; tempos de conexão/leitura/escrita são limitados a cinco segundos. A URL do site é definida na configuração, nunca a partir de um cabeçalho HTTP. `http://localhost:5173` é permitido apenas para desenvolvimento.

## Antes de publicar

1. Conectar um remetente autorizado, sem armazenar credenciais no repositório.
2. Testar chegada real em uma caixa autorizada pelo usuário, inclusive spam, aparência e link no celular.
3. Abrir o link, trocar a senha e confirmar rejeição da senha antiga e das sessões antigas.
4. Reabrir o mesmo link e confirmar rejeição; testar outro link após 15 minutos.
5. Validar HTTPS, URL pública, limites atrás do proxy e acompanhamento de falhas de envio.

Os testes automatizados usam banco H2 isolado e caixa de email falsa em memória; não enviam mensagens reais e não alteram contas da loja.
