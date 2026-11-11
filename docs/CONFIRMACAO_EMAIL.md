# Confirmação de e-mail

Implementação validada localmente; publicação e validação no endereço público são etapas separadas. Confirmação e recuperação usam o mesmo template do EmailJS.

## Configurar o template existente no EmailJS

Editar o template de recuperação configurado em `emailjs.template-id`. Não criar outro e não alterar o serviço/remetente.
- Subject: `Perussi — Confirme seu e-mail ou redefina sua senha` (assunto fixo validado no EmailJS).
- To Email: manter `{{email}}`.
- From Name: `Perussi`.
- From Email / Reply To: manter a configuração que já funciona.
- Content (editor HTML): substituir pelo conteúdo de `docs/emailjs-conta.html` e salvar.

O backend de confirmação envia `confirmacao: true`, com link de confirmação válido por 30 minutos.
A recuperação envia `confirmacao: false`, com link de redefinição válido por 15 minutos.
O backend já publicado envia somente `email` e `link`: a seção invertida `{{^confirmacao}}` mantém o texto da recuperação quando a variável está ausente. Isso permite atualizar o template antes do deploy.
Não substituir a variável por texto `"false"` no Playground: usar booleano false ou remover a variável para testar o formato antigo.

Os textos estão no template; não recebemos HTML do cliente nem usamos variáveis sem escape.
Referência: https://www.emailjs.com/docs/user-guide/dynamic-variables-templates/

Depois de salvar o template, acrescentar ao arquivo privado `backend/emailjs.local.properties`:
```properties
confirmacao.habilitada=true
```
O ID usado é `emailjs.template-id`, o mesmo da recuperação. Não é necessário `emailjs.confirmacao-template-id`.
A URL utiliza `recuperacao.url-site` (local: `http://localhost:5173`). Não versionar os arquivos privados.
Recompilar e reiniciar o backend após configurar. Não ativar a confirmação antes de atualizar o template, pois o antigo mostra somente o texto de recuperação.

No futuro deploy Render, adicionar `CONFIRMACAO_HABILITADA=true`; manter o template e as credenciais existentes e `RECUPERACAO_URL_SITE=https://perussi.vercel.app`. Não publicar antes de testar os dois tipos de mensagem.

## Testar a compatibilidade do template

No Playground/preview do EmailJS, verificar três casos antes de enviar e-mails reais:
1. Apenas `email` e `link`: exibe recuperação, botão Criar nova senha e 15 minutos.
2. `confirmacao: false`, `email` e `link`: mesmo resultado.
3. `confirmacao: true`, `email` e `link`: exibe confirmação, botão Confirmar meu e-mail e 30 minutos.
Conferir também o Subject nos três casos. Renderização e entrega dos dois tipos foram confirmadas pelo usuário no EmailJS. O assunto fixo é igual nos dois casos.

## Comportamento

- Cadastro salva a conta e agenda o envio depois da confirmação da transação. Falha de envio não apaga a conta; reenvio fica disponível no perfil e checkout.
- Contas antigas, com confirmação nula, são consideradas não confirmadas. Dados, pedidos e favoritos são preservados; nenhum envio em massa é feito.
- Cliente pode entrar, navegar e montar carrinho. `/pedidos/finalizar` verifica o estado no banco sob bloqueio do usuário antes de reservar estoque, criar pedido ou limpar carrinho.
- Pedidos anteriores não são cancelados nem alterados retroativamente.
- Troca do e-mail exige senha atual, invalida sessões e links antigos, remove a confirmação e agenda envio ao novo endereço. Nome e senha, sem troca de e-mail, preservam a confirmação.
- Link contém token aleatório de 256 bits no fragmento; apenas SHA-256 fica no banco. Expira em 30 minutos. Reenvio invalida link anterior somente após envio aceito. Falha de provedor reverte a alteração de token.
- Consumo requer ação explícita na página (POST), para evitar consumo por pré-visualizadores de e-mail. Link público não exige login nem autentica o usuário.
- Reenvio autenticado: 60 segundos entre envios e 5 envios por 15 minutos por conta. Há limites adicionais em memória para cadastro/consumo; múltiplas instâncias exigirão limiter compartilhado.
- Ao voltar à aba da loja, o checkout consulta novamente a confirmação; também há botão de atualização manual.

## Verificação manual local

1. Reiniciar backend compilado, banco local e frontend. Atualizar o template compartilhado antes.
2. Criar conta com inbox acessível e verificar chegada do e-mail.
3. Antes de confirmar, entrar e montar carrinho: checkout deve bloquear finalizar sem perder itens.
4. Abrir link e clicar em confirmar. Voltar ao checkout: confirmação deve liberar finalizar.
5. Abrir o mesmo link de novo: não deve permitir segundo consumo.
6. Reenviar, respeitando o intervalo: apenas o último link deve funcionar.
7. Trocar e-mail com senha atual: nova confirmação exigida; links do endereço anterior recusados.
8. Conta antiga deve oferecer reenvio no perfil; conta confirmada não deve repetir confirmação a cada compra.
9. Falha de envio e expiração devem explicar como reenviar; nunca mostrar token, senha ou chave em tela/log.

## Banco

O projeto local usa Hibernate `ddl-auto=update`: as novas colunas nullable são adicionadas ao reiniciar. Antes do deploy, fazer backup do banco; não marcar todas as contas antigas como confirmadas. Sem template configurado, as compras novas ficam bloqueadas para quem ainda não confirmou.

## Validação local concluída em 11/11/2026

O usuário confirmou entrega e consumo do link, bloqueio de checkout sem confirmação, liberação após confirmação, exigência de nova confirmação ao alterar e-mail, recusa de link já usado e invalidação do link anterior após reenvio.

Testes automatizados: 89 testes backend e 7 testes de sessão aprovados; adaptação dos dois envios para template compartilhado verificada em 6 testes de adaptadores. Frontend compilado. Nenhuma cobrança real foi efetuada.
