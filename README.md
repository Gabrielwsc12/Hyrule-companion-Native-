# Hyrule Companion — Android Native (Kotlin + Jetpack Compose)

Migração nativa do arquivo original `hyrule-companion-apk.zip`, **Web Edition 1.16.6**, sem WebView, Capacitor, HTML ou JavaScript em tempo de execução. Mantém o mesmo identificador de aplicativo (`com.hyrulecompanion.app`), o projeto Supabase e a estrutura **schema 13** de dados. A migração preserva o texto editorial original dos 62 jogos, 58 guias, 12 mapas, 5 dungeons e 5 ramificações da cronologia.

## Novidade — 1.16.7-native: avisos de atualização

O Hyrule Companion agora inclui verificação automática na abertura, botão de atualização nas Configurações e notificações opcionais em segundo plano. A integração usa a **nova tabela de releases no Supabase existente**, sem alterar a jornada. **Antes de funcionar em produção**, execute `supabase/APP_UPDATES.sql` no SQL Editor e configure o processo de publicação de APK assinado. Veja o guia passo a passo em **[UPDATES-README.md](UPDATES-README.md)**. Atualizações não são instaladas silenciosamente; o usuário confirma.

## O que foi implementado

- Início: progresso da franquia, TotK em destaque e atalhos.
- Cronologia: cinco ramificações com entradas selecionáveis.
- Atlas: 12 mapas esquemáticos renderizados com Canvas/Path no Compose, marcos geográficos, localização selecionável, filtros, busca, registro de visitas e três camadas do TotK.
- Biblioteca: 62 registros originais, busca, filtros, capa externa opcional e detalhes do jogo com status, datas e notas.
- Guias: 58 guias originais, pesquisa, filtros por tipo, jogo, dificuldade e andamento; etapas marcáveis, checklists de 100%, favoritos e notas.
- Enciclopédia de Dungeons: cinco dungeons, salas, walkthrough, puzzles, inimigos, itens, chefe e checklists persistentes.
- Perfil: progresso, jornadas iniciadas/concluídas, conta, avatar e mural JPEG desenhado com Android Canvas.
- Configurações: tema claro/escuro, abertura com Triforce, música original incluída, foto do perfil, backup JSON e restauração.
- Supabase: criação de conta, entrada, sessão renovável, saída, leitura e upsert em `public.hc_user_data`, resolução explícita de conflito de sincronização.
- Offline: catálogo e progresso são locais; login/Supabase/capas requerem conexão.
- Atualizações: consulta de versões no Supabase ao abrir, aviso Compose, botão de download seguro e notificações opcionais via WorkManager.

## Requisitos

- Android Studio (preferencialmente versão atual compatível com AGP 8.7.3)
- Android SDK Platform 35 e Build Tools correspondentes
- Java/JDK 17
- Gradle 8.9
- Internet na primeira sincronização das dependências do projeto
- Android 8.0 (API 26) ou superior

## Como abrir e gerar o APK

1. Extraia o ZIP da versão nativa e abra a pasta **HyruleCompanionNative** no Android Studio, por `File > Open`.
2. Deixe o Android Studio configurar o **Android SDK 35**, o **JDK 17** e sincronizar os arquivos Gradle.
3. Se pedir uma distribuição Gradle, selecione/instale **Gradle 8.9**. Este pacote não inclui o JAR oficial do Gradle Wrapper porque ele não estava disponível no ambiente em que o projeto foi criado. A partir de uma instalação local Gradle 8.9, gere-o com `gradle wrapper --gradle-version 8.9` (requer um Gradle local instalado). Também pode usar a automação GitHub Actions descrita abaixo, sem wrapper.
4. Em `Build > Build Bundle(s) / APK(s) > Build APK(s)`, gere o APK. Ou no terminal com Gradle instalado: `gradle :app:assembleDebug`.
5. O APK gerado normalmente estará em `app/build/outputs/apk/debug/app-debug.apk`.

### Gerar pela GitHub Actions

Suba os arquivos do projeto para um repositório GitHub, abra **Actions > Build Hyrule Companion Native APK > Run workflow**. O workflow usa Gradle 8.9, executa a checagem do catálogo, compila o app e disponibiliza o APK debug como artifact da execução. Não é necessário publicar nenhuma senha do Supabase para essa compilação: o cliente usa somente a chave **publishable**, igual ao fonte fornecido.

**Aviso sobre instalação:** como o `applicationId` é igual ao original Capacitor, APKs assinados com certificados diferentes NÃO podem ser instalados por cima uns dos outros. Faça **backup JSON** do app antigo antes de desinstalá-lo, ou utilize sua conta Supabase para restaurar a jornada. Os arquivos de `localStorage` da versão Web não migram automaticamente para o armazenamento nativo Android.

## Supabase: preservado sem migração de banco

O app usa, como o HTML original, a mesma URL Supabase e a mesma **chave pública/publishable**. Elas estão em `app/build.gradle.kts` como `BuildConfig` e não exigem outro projeto Supabase.

### Estrutura remota existente

- Autenticação: `POST /auth/v1/signup`, `POST /auth/v1/token?grant_type=password`, renovação `refresh_token`, `POST /auth/v1/logout`.
- Dados: `GET /rest/v1/hc_user_data?select=payload,updated_at&user_id=eq.<uuid>` e `POST /rest/v1/hc_user_data?on_conflict=user_id` com `Prefer: resolution=merge-duplicates`.
- `payload`: objeto `{ "project": "Hyrule Companion", "schemaVersion": 13, "appVersion": "1.16.6", "savedAt": "...", "data": { "hc-progress": "{...}", ... } }`. Note que os valores em `data` são *strings JSON*, exatamente como no código web.
- A tabela `public.hc_user_data`, sua coluna JSONB, os usuários e as RLS permanecem no servidor. O SQL do projeto original foi copiado para `supabase/SUPABASE_SETUP.sql` **apenas como referência**. Não o execute se a estrutura atual já funciona.
- O código do ZIP fornecido NÃO continha chamadas para Edge Functions ou Storage do Supabase, portanto não foi inventada nenhuma API ou operação para eles.

### Chaves locais espelhadas do código original

`hc-progress`, `hc-guide-progress`, `hc-guide-completion`, `hc-guide-favorites`, `hc-guide-notes`, `hc-dungeon-progress`, `hc-dungeon-item-progress`, `hc-atlas-visited`, `hc-user-preferences`.

A conta salva seu token em armazenamento privado do app; `android:allowBackup="false"` evita backup automático de dados privados pelo Android. Não exponha uma chave `service_role` nem uma chave secreta no APK. A chave publishable do código-fonte é pública e o banco segue protegido pelas mesmas políticas RLS do Supabase.

### Escolha entre dados locais e da nuvem

Ao entrar numa conta que já possui jornada remota, aparece a opção **Recuperar nuvem** / **Enviar local** / **Decidir depois**, como no app web. A sincronização manual ou agendada checa se a versão remota foi atualizada depois do último envio antes de sobrescrevê-la. A restauração JSON **não dispara envio automático**; confira o resultado e sincronize manualmente.

## Backup JSON antigo e mural

- **Configurações > Exportar backup JSON:** gera formato com `project`, `version`, `appVersion`, `schemaVersion`, `exportedAt`, `progress`, `guideProgress`, `guideCompletion`, `guideFavorites`, `guideNotes`, `dungeonProgress`, `dungeonItemProgress`, `atlasVisited`, `preferences`.
- **Configurações > Restaurar backup JSON:** aceita backup da Web Edition e snapshots do Supabase (`schemaVersion=13`). Valida ids e limites dos checklists para backups em formato web.
- **Perfil > Personalizar e exportar mural:** JPEG gerado por código nativo; suporte a três temas, retrato/paisagem, nome e tentativa de inserir capas existentes. Capas externas podem falhar sem internet; o mural continua sendo gerado com nomes dos jogos.

## Estrutura

```text
app/src/main/java/com/hyrulecompanion/app/
  updates/                     — consulta de versões, agenda WorkManager e notificações
  MainActivity.kt                — Activity Compose
  HyruleViewModel.kt             — estado da UI, sincronização e escolhas de conflito
  MuralRenderer.kt               — exportação JPEG com Android Canvas
  data/Catalog.kt                — parser do catálogo editorial do HTML de origem
  data/JourneyRepository.kt     — estado local, schema 13, backup/restauração
  network/Supabase.kt           — APIs oficiais Auth e PostgREST via HTTPS
  ui/Design.kt                  — tema medieval, cores, cards
  ui/App.kt                     — navegação, estado da conexão, introdução
  ui/HomeLibrary.kt             — início, biblioteca, detalhes
  ui/TimelineAtlas.kt           — cronologia, mapas Canvas e visitas
  ui/GuidesDungeons.kt          — guias e dungeons
  ui/ProfileSettings.kt         — conta, mural, perfil e configurações
app/src/main/assets/hyrule_content.json — conteúdo editorial original extraído sem reescrita
app/src/main/res/raw/hyrule_intro.mp3   — som fornecido no ZIP original
supabase/SUPABASE_SETUP.sql      — SQL de referência já fornecido no ZIP original
supabase/APP_UPDATES.sql        — nova tabela isolada de releases, execute uma vez
UPDATES-README.md              — instalação, assinatura e publicação de novas versões
.github/workflows/build-native-apk.yml — CI para gerar APK debug
.github/workflows/release-apk.yml — CI para assinar um release estável e anunciar pelo Supabase
```

## Testes e limitações da entrega

Foi executado `python3 tools/check_catalog.py`, verificando contagens, referências cruzadas e disponibilidade dos recursos. O código Kotlin foi analisado estaticamente quanto à estrutura; **não foi possível compilar nem testar o APK neste ambiente**, pois não havia Android SDK nem Gradle/Internet disponíveis. Portanto, esta entrega é **código-fonte Android Studio**, ainda sujeito a correções que possam aparecer na primeira compilação. Não há um APK validado.

A tela de mapas é nativa e esquemática, não substitui imagens vetoriais com precisão geográfica oficial. A versão visual segue a temática dourada/floresta/pergaminho do projeto; não é uma reprodução pixel a pixel do HTML. O mural é renderizado com Android Canvas e não é um espelho exato do canvas Web. A introdução reproduz o MP3 fornecido e pode ser desativada nas preferências. As capas continuam sendo links externos fornecidos no HTML.
