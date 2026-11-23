# Fotos dos produtos

O administrador seleciona uma foto no cadastro; o backend envia os bytes ao Supabase Storage e devolve a URL pública. O produto guarda essa URL. A aplicação não grava novos uploads no disco da Render.

## Configuração

- Bucket `produtos`, público, com limite de 5 MB e tipos `image/jpeg`, `image/png`, `image/webp`.
- Não criar políticas de escrita pública. O endpoint da loja `/api/upload/imagem` exige ADMIN.
- Na Render: `SUPABASE_STORAGE_URL` (URL HTTPS do projeto), `SUPABASE_STORAGE_BUCKET=produtos` e `SUPABASE_STORAGE_SECRET_KEY` (chave secreta `sb_secret_...`).
- Localmente: `backend/storage.local.properties`, ignorado pelo Git, com `storage.supabase.url`, `storage.supabase.bucket` e `storage.supabase.secret-key`.
- A chave não deve estar na Vercel, no frontend, nos commits ou em logs.

Sem configuração, somente o upload retorna indisponível; as demais funcionalidades continuam funcionando. Nomes de arquivos são gerados pelo servidor. O backend verifica tamanho, tipo declarado e assinatura inicial do arquivo. Erros do provedor não são enviados ao cliente.

## Validação após publicar

1. Entrar como administrador e enviar JPG, PNG e WebP de até 5 MB.
2. Salvar produto e conferir a foto em outra sessão sem login.
3. Conferir rejeição de arquivos acima do limite e formatos não aceitos.
4. Conferir que usuário comum não pode enviar imagens.
5. Reiniciar/publicar o backend e conferir novamente a foto.

A troca de foto não apaga automaticamente a anterior; fotos sem produto associado precisam de revisão antes de exclusão. O backup PostgreSQL não inclui os arquivos do Storage: manter cópia separada das imagens.

Esta alteração não converte URLs antigas de localhost nem migra fotos antigas automaticamente.
