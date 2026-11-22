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
- **Conta e Sincronização.** Você pode começar como visitante e jogar com o progresso salvo no aparelho. O catálogo, os guias e os mapas locais ficam disponíveis offline. Com uma conta opcional, é possível sincronizar a jornada entre dispositivos. Se já houver dados na nuvem, o app permite recuperá-los, enviar a jornada local ou decidir depois.
- **Configurações e backup.** Escolha o tema do aparelho, claro ou escuro e a tela de abertura. Ajuste a introdução, sua música e o movimento do avatar. Durante a partida, você pode ocultar spoilers e manter a tela ligada nos guias e mapas. Confira quando ocorreu a última sincronização ou exportação. Exporte um backup JSON para guardar o progresso ou restaure um backup compatível, inclusive da edição web.
- **Atualizações.** O aplicativo verifica se há uma versão mais recente ao abrir. Você também pode conferir manualmente nas Configurações e ativar avisos em segundo plano. Quando houver atualização, o app apresenta as novidades e o acesso ao APK.

Migração nativa do arquivo original `hyrule-companion-apk.zip`, **Web Edition 1.16.6**, sem WebView, Capacitor, HTML ou JavaScript em tempo de execução. Mantém o mesmo identificador de aplicativo (`com.hyrulecompanion.app`), o projeto Supabase e a estrutura **schema 13** de dados. A migração preserva o texto editorial original dos 62 jogos, 58 guias, 12 mapas, 5 dungeons e 5 ramificações da cronologia.

## Novidades

### 1.18.0

- **Início.** Biblioteca, Atlas e Cronologia com ilustrações próprias. O quarto card é um pergaminho com as notas da versão, que pode ser aberto a qualquer momento. O botão Store fica logo abaixo do carrossel. A saudação usa o nome do perfil quando ele está definido.
- **Store e pesca.** Ganhe rúpias verdes no Mar de Hyrule e compre cinco itens. Cosméticos adquiridos podem ser equipados no perfil e ficam visíveis na Comunidade quando o perfil está público. O tema Tears of the Kingdom é ativado em Configurações. A pesca inclui 15 espécies, com duas fanfarras de captura.
- **Guias, atlas e dungeons.** Todos os 62 títulos têm ao menos um guia. Ocarina of Time 3D e Majora’s Mask 3D têm atenção especial às diferenças de edição. Os jogos principais têm cobertura no Atlas e na Enciclopédia. Four Swords, Four Swords Adventures e Tri Force Heroes usam mapas esquemáticos de progressão.
- **Fontes.** [Compêndio de Hyrule](https://www.zeldadungeon.net/wiki/Tears_of_the_Kingdom_Hyrule_Compendium) · [Ocarina of Time 3D](https://www.zeldadungeon.net/ocarina-of-time-walkthrough/) · [Mudanças nos chefes de Majora’s Mask 3D](https://www.zeldadungeon.net/does-majoras-mask-3d-handle-bosses-better-than-the-original/) · [Four Swords Adventures](https://www.zeldadungeon.net/four-swords-adventures-walkthrough/) · [Tri Force Heroes](https://www.zeldadungeon.net/tri-force-heroes-walkthrough/woodlands/)

#### Ativar a Store e preparar a publicação

1. No SQL Editor do projeto Supabase que já atende à Comunidade, execute [STORE_1_18_0.sql](supabase/STORE_1_18_0.sql). A migração adiciona tabelas e funções da Store. Se você executou a primeira revisão da 1.18.0, execute o arquivo atualizado para incorporar os novos recursos.
2. Compile o APK assinado pelo fluxo documentado em [UPDATES-README.md](UPDATES-README.md), com a mesma chave das versões anteriores. A versão que o app anuncia é `1.18.0`, código `11800`.
3. Na atualização 2.00, encerre a venda do Beta tester com `update public.hc_store_flags set beta_on_sale = false where singleton;`. Itens adquiridos continuam equipáveis. O app também limita novas compras na carteira de visitante a versões com código menor que 20000.

Para compilar um APK de teste que instala em paralelo, use `gradle :app:assembleDebug -PhcPreviewApk=true`. Entre com uma conta existente para testar a Store. Para testar o cadastro por e-mail nessa variante, adicione `com.hyrulecompanion.app.preview://auth/confirm` à lista de URLs de redirecionamento do ambiente de teste do Supabase.

#### Configuração privada do criador

No painel Supabase, encontre seu cadastro em **Authentication → Users** e copie o UUID da sua conta. Depois da migração da Store, execute no SQL Editor, trocando `SEU_UUID_AQUI` pelo valor que você conferiu.

```sql
insert into public.hc_store_creator (singleton, user_id)
values (true, 'SEU_UUID_AQUI'::uuid)
on conflict (singleton) do update set user_id = excluded.user_id;
```

Essa linha define a única conta que recebe o emblema Creator e rúpias infinitas. O emblema aparece para visitantes quando o perfil é público.

Os ícones dos peixes são artes do inventário de *Tears of the Kingdom*, da Nintendo, consultadas nas páginas das 15 espécies. [Compêndio de Hyrule no Zelda Dungeon Wiki](https://www.zeldadungeon.net/wiki/Tears_of_the_Kingdom_Hyrule_Compendium).

A carteira de uma conta é mantida no servidor. A carteira de visitante fica separada no aparelho. Guias, mapas e pesca como visitante funcionam sem conexão.

### 1.17.2.1-native

- **Banners animados.** O GIF do banner é reproduzido em loop no Perfil, na prévia do editor e nas visitas da Comunidade. A preferência **Reduzir animações** continua valendo.
- **Atlas.** Toque em um marcador para ler o nome, a categoria e a descrição em uma janela junto ao ponto. A janela acompanha o marcador ao mover ou ampliar o mapa.
- **Editor de perfil.** A pena abre o editor na hora. Se os dados do perfil ainda estiverem carregando, a janela mostra o andamento e permite tentar novamente em caso de falha.
- **Jornada compartilhada.** Visite perfis da Comunidade para ver as capas e o progresso dos jogos. As conquistas **Concluído** e **100%** mostram a Triforce e o ícone de aplausos com a contagem.

### 1.17.2-native

- **Perfil.** Banners JPG, PNG ou GIF de até 1,8 MB. A animação respeita **Reduzir animações**. Um ponto verde indica presença no perfil. Em **Configurações → Privacidade**, ative **Não perturbe** para ocultar a presença de outros jogadores. Toque nos cartões da sua jornada para abrir a ficha do jogo.
- **Início e Atlas.** Carrossel ilustrado com atalhos para Atlas, Cronologia, Biblioteca e Meu mural. O mapa abre em tela cheia com um toque na imagem. A introdução pode ser desativada nas Configurações.
- **Catálogo.** Oito mapas adicionais, incluindo Hyrule de Zelda II e sete áreas de The Minish Cap. São 35 mapas, 216 marcadores, 80 guias e 22 dungeons no total. As 44 edições sem capa e Twilight Princess, cujo link de capa falhava, receberam cards ilustrados próprios disponíveis offline. [Mapas de The Minish Cap](https://zeldacollection.de/en/walkthroughs/the-minish-cap/the-minish-cap-interactive-map/) · [Atlas de Zelda II](https://zeldacollection.de/en/walkthroughs/zelda-ii-the-adventure-of-link/zelda-ii-interactive-map/) · [NESMaps](https://nesmaps.com/maps/Zelda2/Zelda2.html)
- **Referências dos guias.** [The Minish Cap](https://www.zeldadungeon.net/the-minish-cap-walkthrough/) · [Ocarina of Time](https://www.zeldadungeon.net/ocarina-of-time-walkthrough/) · [Majora’s Mask](https://www.zeldadungeon.net/majoras-mask-walkthrough/) · [The Wind Waker](https://www.zeldadungeon.net/the-wind-waker-walkthrough/)

### 1.17.1-native

- **Perfil.** O ícone de pena abre um editor centralizado de foto ou GIF, banner, nome e visibilidade, com prévia do perfil público. O banner é recortado para preencher o fundo do cabeçalho atrás da foto. A Comunidade fica no ícone de duas pessoas ao lado da engrenagem geral.
- **Biblioteca.** O filtro **Só concluídos** inclui jogos marcados como concluídos ou 100% e combina com a busca e o tipo de jogo.
- **Atlas.** Os 27 mapas abrem em tela cheia com zoom, navegação por toque e seleção de marcadores por categoria. Inclui novos pontos como o boneco do Yoshi, a Ilha Bannan e a Ilha do Labirinto.
- **Configurações.** A seção **Notas do criador** reúne a mensagem e o link do Discord.
- **Conta.** A confirmação de e-mail pode retornar ao app. O aviso de conta existente chama o usuário para entrar.

### 1.17.0-native

- **Perfil e Comunidade.** Edite seu nome de usuário e visibilidade em Meu Perfil. Escolha um banner JPG ou PNG de até 1 MB, exibido em proporção 3:1. Na mesma edição, escolha se deseja mostrar seu avatar aos visitantes. GIFs animam também na Comunidade, conforme a preferência de movimento de quem visita.
- **Aparência.** Ocarina of Time, Majora’s Mask, The Wind Waker e Twilight Princess ganharam paletas próprias, combináveis com os modos claro, escuro e do sistema.
- **Guias na Biblioteca.** Cada ficha reúne os guias e dungeons daquele jogo. A Biblioteca também tem acesso à Enciclopédia. A Cronologia exibe capas, plataformas e anos.
- **Conquistas.** A Triforce brilha na primeira vez que você vê a conclusão no mural do Perfil e fica como emblema no cartão do jogo. A exportação JPEG inclui a versão estática do emblema.
- **Notas da versão.** O resumo aparece uma vez por versão instalada. Atualize `app/src/main/assets/release-notes.txt` a cada release para usar o mesmo texto no app, no GitHub e no catálogo de atualizações.
- **Atlas e Enciclopédia.** Sete mapas e 16 marcadores novos para Oracle of Seasons e Phantom Hourglass. As novas dungeons são a Raiz Retorcida e o Templo do Fogo. [Mapas do ZeldaCollection](https://zeldacollection.de/en/maps/)

### 1.16.11-native

- **Comunidade em Perfil.** Configure um nome de usuário, escolha seu Zelda favorito, busque outros aventureiros, siga perfis e consulte jornadas compartilhadas. O perfil começa privado. Você pode disponibilizar o perfil e escolher se também quer exibir os status da sua jornada. Datas, anotações e e-mail ficam fora do perfil público. Use **Atualizar jornada compartilhada** para publicar os status atuais.
- **Aplausos nas conquistas.** Reaja quando outra pessoa concluir um jogo ou chegar a 100%. Cada conta pode deixar um aplauso por jogo e removê-lo depois. Veja quantos aplausos suas próprias conquistas receberam em **Ver meu perfil público**.
- **Atlas ampliado.** Koholint de *Link’s Awakening* (Switch), Hyrule e Lorule de *A Link Between Worlds*, e Labrynna no presente e no passado, além da Passagem Maku, de *Oracle of Ages*. São seis mapas offline e 34 novos pontos. Cada camada tem seu próprio registro de visitas. [Mapas do ZeldaCollection](https://zeldacollection.de/en/maps/)
- **Enciclopédia de Dungeons.** Novas entradas para Caverna da Cauda, Gruta da Garrafa, Casa dos Ventos e Caverna da Sereia. Cada entrada inclui rota, puzzles, ameaças, itens, estratégia do chefe e checklist. Agora também é possível buscar por dungeon, jogo ou região. [Caverna da Cauda](https://www.zeldadungeon.net/links-awakening-walkthrough/tail-cave/), [Gruta da Garrafa](https://www.zeldadungeon.net/links-awakening-walkthrough/bottle-grotto/), [Casa dos Ventos](https://www.zeldadungeon.net/a-link-between-worlds-walkthrough/house-of-gales/) e [Caverna da Sereia](https://www.zeldadungeon.net/oracle-of-ages-walkthrough/mermaids-cave/)

### 1.16.10-native

- **Tema do aplicativo.** Em Configurações → Aparência, escolha **Usar tema do aparelho**, **Claro** ou **Escuro**. Novas instalações acompanham o Android por padrão.
- **Proteção contra spoilers.** A proteção começa ativada. Nos guias e nas dungeons, toque em **Revelar dica** ou no botão da recompensa, da sala ou do chefe para abrir só aquele trecho. A opção fica em **Durante o jogo**.
- **Backup e sincronização.** Confira a data da última exportação JSON registrada neste aparelho e a última sincronização da conta em horário local. Use **Sincronizar agora** ou **Exportar backup JSON** na mesma tela.
- **Acessibilidade.** Os textos acompanham a escala de fonte do Android. **Reduzir animações** pula a introdução e exibe o primeiro quadro do GIF de perfil.
- **Tela ao abrir.** Escolha Início, Biblioteca, Atlas ou Última seção usada. A última seção é lembrada neste aparelho.
- **Tela ligada durante a consulta.** Ative essa opção para ler guias e explorar mapas com o celular ao lado do controle. Ela vem desligada e vale para as áreas de Guias e Atlas enquanto abertas no app.

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

## Retorno ao app após confirmação de e-mail

No mesmo projeto Supabase, adicione `com.hyrulecompanion.app://auth/confirm` em **Authentication → URL Configuration → Redirect URLs**. O e-mail de cadastro deve usar `{{ .ConfirmationURL }}` para que o Supabase confirme o endereço e retorne com o código PKCE de uso único. Abra o link no mesmo aparelho em que iniciou o cadastro. Depois de receber a confirmação no aplicativo, entre com e-mail e senha.

## Ativar banners GIF e presença no Supabase

Execute [COMMUNITY_PRESENCE_AND_BANNERS.sql](supabase/COMMUNITY_PRESENCE_AND_BANNERS.sql) uma vez no SQL Editor do mesmo projeto Supabase, depois das migrações de Comunidade e mídia de perfil. A presença pública exige conta, perfil visível e app em primeiro plano. O modo **Não perturbe** vale para todos os dispositivos da conta. Quando o app perde a conexão, o último sinal de presença expira em até 75 segundos.

## Ativar a Comunidade no Supabase

Para habilitar avatar e banner da versão 1.17, execute [COMMUNITY_PROFILE_MEDIA.sql](supabase/COMMUNITY_PROFILE_MEDIA.sql) uma vez após as duas migrações da Comunidade. Em um ambiente já atualizado para 1.16.11, basta a migração nova se as anteriores já estiverem aplicadas.

Para habilitar a Comunidade na 1.16.11-native, abra o **SQL Editor** do projeto Supabase do Hyrule Companion e execute [COMMUNITY.sql](supabase/COMMUNITY.sql), seguido de [COMMUNITY_REACTIONS.sql](supabase/COMMUNITY_REACTIONS.sql). Execute cada arquivo uma vez. Se já tiver aplicado o primeiro, execute somente o segundo. Essas migrações criam as tabelas e funções da Comunidade.

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

Na 1.16.10-native foram verificadas as preferências de instalação nova e a leitura das opções gravadas pela 1.16.9. O teste de persistência também conferiu a exportação das novas opções no backup JSON e sua recuperação a partir de um snapshot. Os testes de catálogo e das rotinas de atualização e anúncio passaram. O comportamento visual, a suspensão da tela e o fluxo de instalação sobre o APK oficial aguardam validação em aparelho físico.
