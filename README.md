# Hyrule Companion — Android Native (Kotlin + Jetpack Compose)

## Sobre o Aplicativo

O **Hyrule Companion** é um guia de bolso para Android e um diário de jogo para quem explora a série *The Legend of Zelda*. Você pode usá-lo para escolher sua próxima aventura, consultar um mapa durante a partida ou lembrar onde parou depois de semanas longe de Hyrule. Por exemplo, ao jogar *Ocarina of Time*, você pode registrar a data de início, abrir o Atlas pela ficha do jogo e anotar em um guia o que pretende fazer quando voltar.

### Escolha e acompanhe suas aventuras

- **Biblioteca.** Consulte 62 registros por nome, plataforma ou ano e filtre por tipo, incluindo jogos principais, spin-offs, remakes, remasterizações, coleções e DLCs. Cada ficha reúne a descrição do jogo, o status da sua jornada, as datas de início e conclusão e um espaço para notas. Pela ficha você também chega aos guias e, quando disponível, ao mapa daquele título.
- **Início.** Veja quantos jogos estão em andamento, quantos você concluiu e qual porcentagem da biblioteca já terminou. O cartão **Último Zelda concluído** abre a aventura finalizada mais recentemente, de acordo com as datas que você registrou.
- **Cronologia.** Explore cinco grupos da história da série, desde a era anterior à divisão da linha do tempo até os caminhos do herói derrotado, da infância, da vida adulta e da era distante. Toque em um título para consultar sua ficha.

### Consulte o app durante a partida

- **Atlas.** Explore 50 mapas offline de 31 jogos e toque nos marcadores para identificar locais. Em *Tears of the Kingdom*, alterne entre céu, superfície e profundezas. O índice permite buscar marcos, filtrar por região e registrar visitas. Para uma exploração mais detalhada, abra o mapa interativo do ZeldaCollection com conexão à internet.
- **Guias.** Encontre entre 194 guias a ajuda de que precisa para uma missão, um puzzle, um chefe ou uma área de exploração. Abra a ficha do jogo pela Biblioteca e toque em um guia para consultar etapas, favoritos e anotações. Guias com objetivos de 100% têm uma lista própria. As anotações pessoais ajudam a retomar a partida.
- **Enciclopédia de Dungeons.** Consulte 120 fichas de dungeons com rotas por salas, soluções de puzzles, inimigos, itens e tesouros. Acompanhe as etapas, marque o que já recolheu e consulte a estratégia do chefe e a lista final de objetivos.

### Registre, compartilhe e proteja seu progresso

- **Meu Perfil.** Reúna os jogos iniciados e concluídos, consulte datas e notas e personalize seu avatar com uma foto ou um GIF animado de até 1,8 MB. O mural transforma sua jornada em um JPEG para guardar ou compartilhar. Escolha o nome do herói, um dos seis temas, o formato retrato ou paisagem e se deseja incluir as capas.
- **Conta e Sincronização.** Você pode começar como visitante e jogar com o progresso salvo no aparelho. O catálogo, as capas, os guias e os mapas locais ficam disponíveis offline. Com uma conta opcional, é possível sincronizar a jornada entre dispositivos. Se já houver dados na nuvem, o app permite recuperá-los, enviar a jornada local ou decidir depois.
- **Configurações e backup.** Escolha o tema do aparelho, claro ou escuro e a tela de abertura. Ajuste a introdução, sua música e o movimento do avatar. Durante a partida, você pode ocultar spoilers e manter a tela ligada nos guias e mapas. Confira quando ocorreu a última sincronização ou exportação. Exporte um backup JSON para guardar o progresso ou restaure um backup compatível, inclusive da edição web.
- **Atualizações.** O aplicativo verifica se há uma versão mais recente ao abrir. Você também pode conferir manualmente nas Configurações e ativar avisos em segundo plano. Quando houver atualização, o app apresenta as novidades e o acesso ao APK.

Migração nativa do arquivo original `hyrule-companion-apk.zip`, **Web Edition 1.16.6**, sem WebView, Capacitor, HTML ou JavaScript em tempo de execução. Mantém o mesmo identificador de aplicativo (`com.hyrulecompanion.app`), o projeto Supabase e a estrutura **schema 13** de dados. A migração preserva o texto editorial original dos 62 jogos, 58 guias, 12 mapas, 5 dungeons e 5 ramificações da cronologia.

## Novidades da 1.19.0

- **Conquistas.** Sincronização manual dos jogos principais de Zelda com o RetroAchievements. O quadro no perfil público mostra o total e o jogo da última conquista sincronizada.
- **Aquário de Hyrule.** Acompanhe 18 espécies. Peixes lendários aparecem em 0,5% dos lançamentos e rendem de 250 a 500 rúpias.
- **Aventuras.** Publique, comente, aplauda, filtre por pessoas que você segue ou abra o perfil do autor tocando no cabeçalho da publicação.
- **Idiomas.** Português, inglês, espanhol, russo, chinês simplificado e japonês em Configurações.
- **Forma definitiva de jogar.** A ficha dos jogos principais tem um atalho para recomendações de plataforma, emulador, traduções e mods com links dos responsáveis pelos projetos.
- **Perfil.** O cabeçalho reaproveita o nome e o banner salvos. Chama e névoa contornam o nome, e você pode usar um efeito de cada vez.


Artes de referência das novas espécies no Zelda Dungeon Wiki. [Hylian Loach](https://www.zeldadungeon.net/wiki/Hylian_Loach), [Ordon Catfish](https://www.zeldadungeon.net/wiki/Ordon_Catfish) e [Reekfish](https://www.zeldadungeon.net/wiki/Reekfish).

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

## Retorno ao app após confirmação de e-mail

No mesmo projeto Supabase, adicione `com.hyrulecompanion.app://auth/confirm` em **Authentication → URL Configuration → Redirect URLs**. O e-mail de cadastro deve usar `{{ .ConfirmationURL }}` para que o Supabase confirme o endereço e retorne com o código PKCE de uso único. Abra o link no mesmo aparelho em que iniciou o cadastro. Depois de receber a confirmação no aplicativo, entre com e-mail e senha.

## Ativar banners GIF e presença no Supabase

Execute [COMMUNITY_PRESENCE_AND_BANNERS.sql](supabase/COMMUNITY_PRESENCE_AND_BANNERS.sql) uma vez no SQL Editor do mesmo projeto Supabase, depois das migrações de Comunidade e mídia de perfil. A presença pública exige conta, perfil visível e app em primeiro plano. O modo **Não perturbe** vale para todos os dispositivos da conta. Quando o app perde a conexão, o último sinal de presença expira em até 75 segundos.

## Ativar as funções online da 1.19.0

No Supabase existente, aplique primeiro os scripts das versões anteriores que ainda faltarem. Depois, execute `supabase/STORE_1_19.sql`, `supabase/RETRO_1_19.sql`, `supabase/ADVENTURES_1_19.sql` e `supabase/POLISH_1_19.sql` nessa ordem. As novas funções também podem ser avaliadas em um projeto de teste que já tenha a mesma estrutura.

Implante a função `retro-sync` a partir da pasta do projeto com `supabase functions deploy retro-sync --project-ref SEU_PROJECT_REF`. Ela usa as variáveis padrão do Supabase, valida a sessão do Hyrule Companion e confirma o vínculo com um código temporário no campo User motto do RetroAchievements. A chave do RetroAchievements é fornecida no momento da sincronização e o banco guarda somente o resumo de progresso. O perfil precisa estar público para que outros usuários vejam esse resumo ou para que o dono publique e comente em Aventuras.

Se já ativou a 1.19.0 inicial, reaplique o `RETRO_1_19.sql` desta revisão e execute `POLISH_1_19.sql`. Em seguida, atualize a função existente `retro-sync` usando o `supabase/functions/retro-sync/index.ts` do pacote e sincronize novamente pelo app para preencher o jogo da última conquista.

O Aquário importa as capturas creditadas que ainda estão no histórico do servidor. A coleção é registrada a cada nova captura.

## Ativar a Comunidade no Supabase

Para configurar a Comunidade pela primeira vez, execute [COMMUNITY.sql](supabase/COMMUNITY.sql), [COMMUNITY_REACTIONS.sql](supabase/COMMUNITY_REACTIONS.sql) e [COMMUNITY_PROFILE_MEDIA.sql](supabase/COMMUNITY_PROFILE_MEDIA.sql) nessa ordem. Depois configure presença e banners conforme a seção anterior.

Se a Comunidade já estiver ativa, o arquivo opcional [COMMUNITY_MEDIA_CACHE.sql](supabase/COMMUNITY_MEDIA_CACHE.sql) habilita a reutilização das fotos e dos banners entre sessões, com verificação de permissão e revisão a cada abertura de perfil. Aplique na ordem das migrações acima e reabra o app. Sem essa migração, a Comunidade continua usando a consulta já existente.

Para verificar a integração em um ambiente de teste, configure a URL e a chave publishable desse ambiente em `app/build.gradle.kts`. Use duas contas para conferir a busca, a visibilidade da jornada, a opção de seguir e os aplausos.

## Supabase: contas e sincronização da jornada

O app usa, como o HTML original, a mesma URL Supabase e a mesma **chave pública/publishable**. Elas estão em `app/build.gradle.kts` como `BuildConfig` e não exigem outro projeto Supabase.

### Estrutura remota existente

- Autenticação: `POST /auth/v1/signup`, `POST /auth/v1/token?grant_type=password`, renovação `refresh_token`, `POST /auth/v1/logout`.
- Dados: `GET /rest/v1/hc_user_data?select=payload,updated_at&user_id=eq.<uuid>` e `POST /rest/v1/hc_user_data?on_conflict=user_id` com `Prefer: resolution=merge-duplicates`.
- `payload`: objeto `{ "project": "Hyrule Companion", "schemaVersion": 13, "appVersion": "1.16.6", "savedAt": "...", "data": { "hc-progress": "{...}", ... } }`. Note que os valores em `data` são *strings JSON*, exatamente como no código web.
- A tabela `public.hc_user_data`, sua coluna JSONB, os usuários e as RLS permanecem no servidor. O SQL do projeto original foi copiado para `supabase/SUPABASE_SETUP.sql` **apenas como referência**. Não o execute se a estrutura atual já funciona.
- O código do ZIP fornecido NÃO continha chamadas para Edge Functions ou Storage do Supabase, portanto não foi inventada nenhuma API ou operação para eles.

### Chaves locais espelhadas do código original

`hc-progress`, `hc-guide-progress`, `hc-guide-completion`, `hc-guide-favorites`, `hc-game-favorites`, `hc-guide-notes`, `hc-dungeon-progress`, `hc-dungeon-item-progress`, `hc-atlas-visited`, `hc-user-preferences`.

A conta salva seu token em armazenamento privado do app; `android:allowBackup="false"` evita backup automático de dados privados pelo Android. Não exponha uma chave `service_role` nem uma chave secreta no APK. A chave publishable do código-fonte é pública e o banco segue protegido pelas mesmas políticas RLS do Supabase.

### Escolha entre dados locais e da nuvem

Ao entrar numa conta que já possui jornada remota, aparece a opção **Recuperar nuvem** / **Enviar local** / **Decidir depois**, como no app web. A sincronização manual ou agendada checa se a versão remota foi atualizada depois do último envio antes de sobrescrevê-la. A restauração JSON **não dispara envio automático**; confira o resultado e sincronize manualmente.

## Backup JSON antigo e mural

- **Configurações > Exportar backup JSON:** gera formato com `project`, `version`, `appVersion`, `schemaVersion`, `exportedAt`, `progress`, `guideProgress`, `guideCompletion`, `guideFavorites`, `gameFavorites`, `guideNotes`, `dungeonProgress`, `dungeonItemProgress`, `atlasVisited`, `preferences`.
- **Configurações > Restaurar backup JSON:** aceita backup da Web Edition e snapshots do Supabase (`schemaVersion=13`). Valida ids e limites dos checklists para backups em formato web.
- **Perfil > Personalizar e exportar mural:** JPEG gerado por código nativo com seis temas, opções de retrato e paisagem, nome e capas do catálogo disponíveis offline.

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

## Verificar esta versão

- Em **Configurações → Durante o jogo**, ligue o feedback tátil e toque em **Testar vibração da pesca**.
- Durante uma rodada, confira a fisgada, os puxões e a fuga. Desligue o feedback tátil para confirmar que os efeitos ficam desativados.
