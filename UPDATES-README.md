# Atualizações automáticas — Hyrule Companion

Esta edição adiciona **detecção automática de novas versões e notificação de atualização**. Ela **não instala APK silenciosamente**, o que não é permitido em aplicativos Android comuns. O usuário toca **Atualizar agora** para baixar o novo APK em HTTPS e confirma a instalação no Android.

## Como funciona

1. O app lê a versão instalada usando `BuildConfig.VERSION_CODE`.
2. Ao abrir/retomar o app, consulta a tabela pública `public.hc_app_releases` do **mesmo Supabase do Hyrule Companion** e seleciona a maior `version_code` ativa.
3. Se a versão publicada for maior, mostra um diálogo com número de versão, novidades e botões **Atualizar agora** / **Lembrar depois**. Após **Lembrar depois**, respeita 24 horas antes de repetir o diálogo (a verificação manual ignora esse prazo).
4. Em **Configurações > Atualizações do aplicativo**, o usuário pode verificar imediatamente e ativar notificações em segundo plano.
5. O WorkManager verifica em segundo plano aproximadamente a cada **12 horas** quando os avisos estão ativados. O Android pode atrasar essa execução (Doze/economia de bateria). No Android 13+, o usuário precisa conceder a permissão de notificações. O sistema envia somente **um aviso de sistema por versão**.
6. O usuário toca o aviso e é levado ao aplicativo para baixar a atualização. Não há mudança nos dados da jornada, nas tabelas de progresso ou nas contas Supabase.

## Configuração inicial obrigatória (uma vez só)

### 1. Preparar o catálogo de releases no Supabase EXISTENTE

No painel do projeto Supabase atual, abra **SQL Editor** e execute **`supabase/APP_UPDATES.sql`**. Isso cria **somente** `public.hc_app_releases`, com leitura pública protegida por RLS e sem escrita por usuários. Não execute `SUPABASE_SETUP.sql` de novo. Não cria outro banco nem altera `hc_user_data`.

### 2. Publicar seu projeto em um repositório GitHub

Crie um repositório GitHub para `HyruleCompanionNative` e suba o conteúdo da pasta do projeto (com `app/`, `.github/`, `supabase/` etc. na raiz). O workflow de release usa uma URL HTTPS pública do **GitHub Releases**. Portanto, publique os APKs em releases acessíveis aos usuários. Repositórios privados podem impedir o download por usuários não autenticados.

### 3. Assinatura: use SEMPRE a mesma chave

APK de atualização exige **mesmo applicationId** (`com.hyrulecompanion.app`) e **mesmo certificado de assinatura** do APK instalado, além de código de versão superior. O APK debug gerado no GitHub Actions anterior tem chave debug efêmera e **não é apropriado como canal de atualização**.

Crie uma chave de assinatura privada, com backup seguro e fora do repositório. Exemplo em um computador com JDK:

```bash
keytool -genkeypair -v -keystore hyrule-release.jks -alias hyrule -keyalg RSA -keysize 3072 -validity 10000
```

Nunca envie a chave ou a senha pelo chat. Nunca comite `*.jks` nem `*.keystore`.

Adicione os segredos no GitHub (**Settings > Secrets and variables > Actions**):

| Segredo | Valor |
| --- | --- |
| `HC_KEYSTORE_BASE64` | Conteúdo base64 do seu `hyrule-release.jks` (`base64 -w0 hyrule-release.jks` em Linux) |
| `HC_KEYSTORE_PASSWORD` | Senha do keystore |
| `HC_KEY_ALIAS` | Alias (ex.: `hyrule`) |
| `HC_KEY_PASSWORD` | Senha da chave |
| `HC_SUPABASE_SECRET_KEY` | Chave **secreta** `sb_secret_...` do mesmo Supabase, acessível **somente** como segredo de CI |

**Segurança:** `HC_SUPABASE_SECRET_KEY` nunca entra no código Kotlin nem no APK; só é lida pelo workflow privado para cadastrar releases. O app usa a chave publishable pública para leitura da tabela. Se preferir não usar a chave secreta no GitHub, retire a etapa de anúncio automático do workflow e use o SQL manual descrito abaixo.

### 4. Criar e anunciar uma versão

Antes de gerar cada APK:

1. Atualize `versionCode` (inteiro **estritamente maior** que o anterior) e `versionName` no arquivo `app/build.gradle.kts`. A edição atual usa `1.18.1.1` (`11811`).
2. Commit/push do código.
3. Crie uma tag **exatamente igual a `v` + versionName** e faça push. Para esta versão, use `git tag v1.18.1.1 && git push origin v1.18.1.1`.
4. A GitHub Action **`Release Hyrule Companion APK and announce update`** compila e assina o APK, cria GitHub Release pública e publica a versão em `hc_app_releases` (somente se os segredos e a tabela já tiverem sido configurados).
5. Usuários que têm o verificador instalado verão o aviso quando abrirem o app ou receberão a notificação em segundo plano, se habilitada.

Usuários de edições sem o verificador podem instalar manualmente um APK oficial compatível que inclua esse recurso.

### Alternativa: publicar sem GitHub Actions automatizado

É possível hospedar o APK por HTTPS em GitHub Releases ou em um bucket **público** de Storage do Supabase. Depois, execute no SQL Editor:

```sql
insert into public.hc_app_releases
(version_code, version_name, notes, download_url, mandatory, active)
values
(11811, '1.18.1.1', 'Correção da vibração na pesca.',
 'https://SEU-HOST-HTTPS/HyruleCompanion-1.18.1.1.apk', false, true)
on conflict (version_code) do update set
 version_name = excluded.version_name,
 notes = excluded.notes,
 download_url = excluded.download_url,
 mandatory = excluded.mandatory,
 active = true;
```

**Use dados reais.** A publicação deve acontecer **depois** que o APK assinado estiver disponível ao público. `mandatory=true` destaca que a atualização é importante, mas não bloqueia o uso offline.

## Como testar sem anunciar uma atualização falsa a todos

- Em um projeto de teste com o SQL aplicado, instale um APK oficial com código inferior a `11811` e anuncie o APK da `1.18.1.1` assinado pela mesma chave.
- Insira esse release com URL pública HTTPS na tabela. Abra o app `11607`. O diálogo deve surgir.
- Teste **Lembrar depois**, a busca manual nas Configurações, o botão para o download e a permissão para notificação em segundo plano.
- Instale `11608` por cima e confira que a jornada local permanece intacta. **Nunca** assine a atualização com outro certificado.
- Teste sem internet: o app deve continuar funcionando e a verificação de versões não deve afetar a jornada.

## Limitações

- Não existe instalação silenciosa de APK para apps normais distribuídos fora do Google Play. O usuário confirma a instalação e pode precisar autorizar o navegador como fonte de instalação.
- A verificação ao abrir é imediata na melhor hipótese; background via WorkManager é **aproximado**, não tempo real, e depende de permissão e bateria.
- Se no futuro o app for publicado no Google Play, o mecanismo ideal passa a ser o **Play In-App Updates** para os usuários instalados pela Play Store.
- Esta entrega é **código-fonte**; ambiente de execução atual não possui Android SDK/Gradle para compilar e validar o APK.
