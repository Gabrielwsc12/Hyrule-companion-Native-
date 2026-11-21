# Hyrule Companion — Android Native (Kotlin + Jetpack Compose)

## Sobre o Aplicativo

O **Hyrule Companion** é um guia de bolso para Android e um diário de jogo para quem explora a série *The Legend of Zelda*. Você pode usá-lo para escolher sua próxima aventura, consultar um mapa durante a partida ou lembrar onde parou depois de semanas longe de Hyrule. Por exemplo, ao jogar *Ocarina of Time*, você pode registrar a data de início, abrir o Atlas pela ficha do jogo e anotar em um guia o que pretende fazer quando voltar.

### Escolha e acompanhe suas aventuras

- **Biblioteca.** Consulte 62 registros por nome, plataforma ou ano e filtre por tipo, incluindo jogos principais, spin-offs, remakes, remasterizações, coleções e DLCs. Cada ficha reúne a descrição do jogo, o status da sua jornada, as datas de início e conclusão e um espaço para notas. Pela ficha você também chega aos guias e, quando disponível, ao mapa daquele título.
- **Início.** Veja quantos jogos estão em andamento, quantos você concluiu e qual porcentagem da biblioteca já terminou. O cartão **Último Zelda concluído** abre a aventura finalizada mais recentemente, de acordo com as datas que você registrou.
- **Cronologia.** Explore cinco grupos da história da série, desde a era anterior à divisão da linha do tempo até os caminhos do herói derrotado, da infância, da vida adulta e da era distante. Toque em um título para consultar sua ficha.

### Consulte o app durante a partida

- **Atlas.** Explore 14 mapas offline de 12 jogos e toque nos 134 marcadores para identificar locais. Em *Tears of the Kingdom*, alterne entre céu, superfície e profundezas. O índice permite buscar marcos, filtrar por região e registrar visitas. Para uma exploração mais detalhada, abra o mapa interativo do ZeldaCollection com conexão à internet.
- **Guias.** Encontre entre 58 guias a ajuda de que precisa para uma missão, um puzzle, um chefe ou uma área de exploração. Pesquise por região ou tema, filtre por jogo, dificuldade e andamento, salve favoritos e marque as etapas conforme avança. Guias com objetivos de 100% têm uma lista própria. As anotações pessoais ajudam a retomar a partida.
- **Enciclopédia de Dungeons.** Consulte cinco dungeons com rotas por salas, soluções de puzzles, inimigos, itens e tesouros. Acompanhe as etapas, marque o que já recolheu e consulte a estratégia do chefe e a lista final de objetivos.

### Registre, compartilhe e proteja seu progresso

- **Meu Perfil.** Reúna os jogos iniciados e concluídos, consulte datas e notas e personalize seu avatar com uma foto ou um GIF animado de até 1,8 MB. O mural transforma sua jornada em um JPEG para guardar ou compartilhar. Escolha o nome do herói, um dos seis temas, o formato retrato ou paisagem e se deseja incluir as capas.
- **Conta e Sincronização.** Você pode começar como visitante e jogar com o progresso salvo no aparelho. O catálogo, os guias e os mapas locais ficam disponíveis offline. Com uma conta opcional, é possível sincronizar a jornada entre dispositivos. Se já houver dados na nuvem, o app permite recuperá-los, enviar a jornada local ou decidir depois.
- **Configurações e backup.** Escolha o tema claro ou escuro e ajuste a introdução e sua música. Exporte um backup JSON para guardar o progresso ou restaure um backup compatível, inclusive da edição web.
- **Atualizações.** O aplicativo verifica se há uma versão mais recente ao abrir. Você também pode conferir manualmente nas Configurações e ativar avisos em segundo plano. Quando houver atualização, o app apresenta as novidades e o acesso ao APK.

Migração nativa do arquivo original `hyrule-companion-apk.zip`, **Web Edition 1.16.6**, sem WebView, Capacitor, HTML ou JavaScript em tempo de execução. Mantém o mesmo identificador de aplicativo (`com.hyrulecompanion.app`), o projeto Supabase e a estrutura **schema 13** de dados. A migração preserva o texto editorial original dos 62 jogos, 58 guias, 12 mapas, 5 dungeons e 5 ramificações da cronologia.

## Novidades

### 1.16.9-native

- **Início.** O cartão **Último Zelda concluído** mostra a capa do jogo com a data de conclusão mais recente. Jogos com status **Concluído** ou **100%** entram na seleção. Toque no cartão para abrir o jogo. Quem ainda não tem uma conclusão datada encontra um atalho para a Biblioteca.
- **Perfil.** Os controles para escolher, trocar e remover a foto ficam junto ao avatar, em **Meu Perfil**. GIFs animados são reproduzidos onde a foto de perfil aparece. O limite por arquivo é de 1,8 MB.
- **Exportação da jornada.** O mural JPEG ganhou os temas **Grande Mar**, **Montanha da Morte** e **Crepúsculo**. Com Floresta, Noite e Pergaminho, são seis opções para exportar em retrato ou paisagem.
- **Primeiro acesso.** Um aviso explica que criar conta é opcional. **Criar conta** abre o formulário no Perfil. **Continuar como visitante** permite começar na hora e salvar o progresso no aparelho. A escolha fica registrada para as próximas aberturas.
- **Conta e Sincronização.** A seção de conta passa a ter esse nome na interface. É onde o jogador entra com sua conta e sincroniza a jornada.
- **Atlas.** São 14 mapas offline para 12 jogos, com 134 pontos selecionáveis baseados nos mapas do ZeldaCollection. Em *Tears of the Kingdom*, é possível alternar entre céu, superfície e profundezas. Alguns pontos identificam a região ou a entrada do local. O botão **Explorar mapa** abre o mapa interativo completo online. Ao entrar no Atlas pela ficha de um jogo, o mapa correspondente já aparece selecionado.

### Novidade — 1.16.7-native: avisos de atualização

O Hyrule Companion agora inclui verificação automática na abertura, botão de atualização nas Configurações e notificações opcionais em segundo plano. A integração usa a **nova tabela de releases no Supabase existente**, sem alterar a jornada. **Antes de funcionar em produção**, execute `supabase/APP_UPDATES.sql` no SQL Editor e configure o processo de publicação de APK assinado. Veja o guia passo a passo em **[UPDATES-README.md](UPDATES-README.md)**. Atualizações não são instaladas silenciosamente; o usuário confirma.

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
- **Perfil > Personalizar e exportar mural:** JPEG gerado por código nativo; suporte a seis temas, retrato/paisagem, nome e tentativa de inserir capas existentes. Capas externas podem falhar sem internet; o mural continua sendo gerado com nomes dos jogos.

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
  ui/TimelineAtlas.kt           — cronologia, mapas offline e visitas
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

Na preparação da 1.16.9-native, o APK de depuração compilou com `gradle :app:assembleDebug`. Passaram as verificações do catálogo, da configuração de atualização e do anúncio de versão. A animação dos GIFs e a navegação por toque ainda aguardam validação em aparelho Android.
