# Perussi

Conexão com Mercado Pago Orders e aprovação de PIX fictício verificadas com PostgreSQL isolado. O recebimento de webhook foi validado pelo simulador oficial; a entrega espontânea ainda não foi confirmada. Resultados e limites em [TESTE_MERCADO_PAGO.md](TESTE_MERCADO_PAGO.md).

Pagamentos pendentes com ID remoto são consultados automaticamente como recuperação para notificações ausentes. O intervalo entre ciclos é de 60 segundos (`pagamentos.conciliacao.intervalo-ms`); o tempo efetivo depende das chamadas anteriores e da disponibilidade do provedor. A confirmação exige valor e vínculo corretos. Erros mantêm a reserva e são tentados novamente. Pedidos sem ID remoto exigem recuperação pelo fluxo existente ou conciliação manual. Para grande volume, usar paginação e controle de taxa antes de ampliar essa rotina.

Loja virtual com React 18, TypeScript, Vite, Tailwind, Spring Boot 3.4 e PostgreSQL.

## Pasta principal

Trabalhe em `frontend/` e `backend/` desta raiz. A subpasta `Perussi-main/` é uma cópia anterior preservada e não recebe estas correções. Ela ainda contém as configurações e falhas antigas; não a use para executar ou publicar o projeto.

## Requisitos

- Node.js 22 LTS e npm.
- JDK 17 ou 21 (o projeto compila para Java 17).
- PostgreSQL, banco `perussi`, por padrão em `localhost:5433`.
- Credenciais de teste do Mercado Pago para testar PIX.

O Maven Wrapper em `backend/` baixa a versão 3.9.9 no primeiro uso; não é necessário instalar Maven globalmente. O primeiro build precisa de acesso à internet.

## Frontend

```powershell
cd frontend
npm ci
npm run dev
```

Abra `http://localhost:5173`. Para compilar para publicação:

```powershell
npm run build
```

O resultado fica em `frontend/dist`. O TypeScript verifica os tipos sem gerar JavaScript junto aos fontes. A API padrão é `http://localhost:8080/api`; para mudar, configure `VITE_API_URL` em `frontend/.env.local` antes de iniciar/compilar. Nunca coloque segredos em variáveis `VITE_*`, pois elas são incluídas no navegador.

## Backend

Defina as variáveis no mesmo terminal PowerShell usado para executar o backend:

```powershell
cd backend
$env:DB_PASSWORD = 'senha-do-seu-postgresql'
$env:MERCADOPAGO_ACCESS_TOKEN = 'credencial-de-teste-do-mercado-pago'
$env:MERCADOPAGO_NOTIFICATION_URL = 'https://seu-endereco-publico/api/webhooks/mercadopago'

# Gera uma chave aleatória para desenvolvimento sem imprimi-la.
$jwtBytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)

.\mvnw.cmd spring-boot:run
```

Em Linux/macOS, use `./mvnw` e exporte as mesmas variáveis. Arquivos `.env` não são carregados automaticamente pelo backend.

`JWT_SECRET` é obrigatório, sem chave padrão, e deve conter no mínimo 32 bytes de material aleatório. Em um ambiente persistente, armazene a chave em um gerenciador de segredos e reutilize-a entre reinicializações/instâncias. Trocar a chave invalida tokens anteriores; os usuários precisam entrar novamente. Não reutilize a chave antiga presente na cópia de segurança.

Para outro banco, configure também `SPRING_DATASOURCE_URL` e `SPRING_DATASOURCE_USERNAME`. As configurações atuais de `ddl-auto=update`, logs SQL, CORS localhost e uploads locais são de desenvolvimento. Preparação de produção permanece pendente.

## Testes

```powershell
cd backend
.\mvnw.cmd test
```

Os testes de segurança usam mocks, sem PostgreSQL ou cobranças reais. Cobrem rejeição de pagamento manual, autorização administrativa, identidade do cliente na criação de PIX, campos obrigatórios, pedidos de outro usuário, pedidos já pagos e validade/assinatura de JWT.

## Situação das correções

A primeira etapa remove a confirmação direta de pagamento pelo cliente, bloqueia explicitamente a rota antiga, verifica o dono do pedido ao criar PIX, externaliza senha do banco/chave JWT, retorna 401 para tokens inválidos e corrige a configuração de tipos do frontend.

A segunda etapa implementa reutilização de PIX, confirmação sem regressão de status, cancelamento seguro, reserva e baixa atômica de estoque. O pagamento real continua dependendo de configuração e validação da conta/webhook Mercado Pago. Não foi efetuada compra real. Permanecem pendentes frete, retomada de pagamento pela lista de pedidos, sessão após alteração de e-mail, verificação da assinatura do webhook e tratamento completo de estornos. Cartão não está integrado. Não publicar para vendas reais antes dos testes completos.


Os testes Java foram executados com Maven 3.9.15 disponível na extensão Java local; o wrapper gerado fixa 3.9.9. A instalação npm também reportou 10 alertas de dependências (1 baixo, 2 moderados e 7 altos), cuja avaliação/atualização ainda está pendente. Não foi aplicado `npm audit fix` automaticamente.

## Pagamento e reserva de estoque

Validação em 28/09/2026: frontend compilado (`npm run build`) e 29 testes Java aprovados, sem falhas ou testes ignorados. Inclui concorrência pela última unidade, checkout duplicado, devolução única de estoque, rollback, arredondamento, PIX reutilizado, aprovação durante cancelamento e falhas do provedor/webhook.

- Novos pedidos reservam estoque por 30 minutos; altere `RESERVA_MINUTOS` para outro prazo. Um agendador verifica os vencidos a cada minuto, enquanto o backend estiver funcionando.
- A reserva usa atualização condicional no banco. O checkout bloqueia o usuário durante a transação para evitar dois pedidos do mesmo carrinho; alterações de várias variações seguem uma ordem estável.
- O preço unitário com desconto é arredondado para centavos antes de multiplicar pela quantidade, tanto no carrinho quanto no pedido.
- A criação de PIX registra a intenção antes da chamada externa. Usa o UUID do pedido como chave de idempotência e referência externa. Consultas seguintes recuperam o mesmo pagamento; não existe botão que substitua a cobrança por outra.
- Configure `MERCADOPAGO_NOTIFICATION_URL` com a URL HTTPS pública correta. O endereço antigo de ngrok foi retirado. O backend agora envia essa configuração na criação do PIX. Sem URL, é necessário configurar as notificações externamente no provedor.
- O webhook consulta o provedor, valida vínculo/valor e bloqueia o pedido durante a atualização. Falhas retornam erro HTTP, permitindo reenvio. Uma notificação aprovada repetida não faz um pedido enviado/entregue voltar para pago.
- O painel permite pendente → cancelado, pago → enviado e enviado → entregue. Pagamento aprovado só vem da consulta ao provedor. Pedido pago exige fluxo de estorno, ainda manual.
- Ao cancelar/expirar, um PIX existente é consultado e, se necessário, cancelado no provedor antes da liberação. Se o pagamento foi aprovado durante a tentativa de cancelar, o pedido fica pago e a reserva é preservada. Cancelamentos repetidos devolvem o estoque uma única vez.
- Em timeout ou resultado incerto da criação/cancelamento, o estoque NÃO é liberado por suposição. O agendador tenta conciliar novamente. Intenção de pagamento sem resultado recuperável exige revisão manual; consultar os avisos de reserva mantida nos logs. Isso também cobre pedidos cuja solicitação foi registrada, mas a aplicação caiu antes de chamar o provedor.
- A tela de pagamento consulta o status local a cada cinco segundos e volta aos pedidos quando o pedido deixa de estar pendente. A atualização depende do webhook ou da conciliação do agendador ao vencer a reserva.

As colunas nullable `reserva_expira_em` e `pagamento_solicitado` são acrescentadas pelo Hibernate na próxima inicialização de desenvolvimento. Não foram aplicadas alterações ao banco real nesta etapa. Pedidos antigos sem prazo não expiram automaticamente e precisam de revisão. Um identificador de PIX que já tenha sido sobrescrito na versão anterior não pode ser reconstruído a partir do banco atual; essas cobranças exigem conciliação no Mercado Pago. Faça backup e planeje migrações antes de produção.

Os testes de integração usam H2 em memória para executar transações concorrentes, sem acessar PostgreSQL nem cobrar no Mercado Pago. Ainda é necessário validar esses cenários em PostgreSQL isolado e no ambiente de testes do provedor, incluindo indisponibilidade e cancelamento simultâneo ao pagamento.

Referência da integração: [PIX e chave de idempotência na documentação do Mercado Pago](https://www.mercadopago.com.br/developers/pt/docs/checkout-api-payments/integration-configuration/integrate-pix?scope=prod).

### Entrega local
Novos pedidos: apenas Curitiba/PR, validada pelo CEP no servidor; frete fixo de R$ 15,00 incluído no total do PIX. Fora de Curitiba a finalização é bloqueada. Pedidos anteriores não recebem frete retroativo. Detalhes e validação em TESTE_MERCADO_PAGO.md (28/10/2026).
