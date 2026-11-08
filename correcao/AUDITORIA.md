# Auditoria do FieldService — 16/10/2026

Branch auditada: `feat/ui-ux-redesign` (commit `f68d691`).

Build sem erros, **11/11 testes unitários passando** e app testado no emulador Pixel_10a:
login, fluxo de atendimento, filtros, perfil, temas, rotação e o caso em que o Android
encerra o app em segundo plano. Nenhum código foi alterado durante a auditoria.

## Como compilar e rodar

- Use o Java do Android Studio (o Java 26 do sistema é novo demais para o Gradle):
  ```bash
  export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
  ./gradlew assembleDebug testDebugUnitTest
  ```
- Instalar no emulador:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- Credenciais de teste: `tecnico@fieldservice.com` ou `matheus@fieldservice.com`, senha `123456`.

## Funcionalidades que funcionam

| Tela | O que foi testado | Resultado |
|---|---|---|
| **Login** | Campos vazios, senha errada, login válido, maiúsculas no e-mail | ✅ Mensagens de erro corretas; o e-mail aceita maiúsculas |
| **Início** | Saudação, resumo do dia (pendentes, em atendimento, concluídos), até 3 chamados de prioridade alta ou crítica | ✅ Os números se atualizam quando o status de um chamado muda |
| **Chamados** | Lista com os filtros Todos, Pendentes, Em atendimento e Concluídos, com a contagem em cada um | ✅ |
| **Detalhes** | Linha do tempo e botão que muda conforme o status: Aceitar → Iniciar deslocamento → Registrar chegada → Iniciar atendimento → Finalizar | ✅ Fluxo completo no #1028 até "Concluído" |
| **Perfil** | Dados do técnico, Notificações, Tema (Escuro, Claro, Super Dark), Sobre, Sair | ✅ O tema Claro é aplicado na hora |
| **Rotação** | Girar a tela com um filtro ativo | ✅ O filtro e a lista continuam iguais |

## Problemas encontrados

### Importantes

- [ ] **1. O chamado mais urgente não tem ação.**
  O #1031 (crítico, status Aberto) aparece em primeiro nos prioritários, mas a tela de
  detalhes não mostra botão nenhum e a linha do tempo fica toda cinza.
  - Causa: `TicketAction.forStatus` não tem um caso para `OPEN`
    (`app/src/main/java/com/fieldservice/app/presentation/ticketdetails/TicketAction.java`).
  - Correção sugerida: adicionar `case OPEN:` junto de `case ASSIGNED:` (ação "Aceitar chamado"
    → `ACCEPTED`) e tratar `OPEN` como o primeiro passo da linha do tempo em
    `TicketDetailsActivity.renderTimeline`.

- [ ] **2. A sessão se perde quando o Android encerra o app.**
  Com o app em segundo plano encerrado pelo sistema, ao reabrir ele volta para a tela
  principal, mas a saudação fica "Olá, 👋" sem nome e o Perfil aparece sem nome e sem e-mail.
  O andamento dos chamados também se perde (esperado enquanto os dados forem simulados).
  - Causa: o usuário logado só existe na memória (`MockAuthRepository.loggedInTechnician`)
    e nada redireciona para o login quando ele está vazio.
  - Correção sugerida: guardar o e-mail logado nas preferências do app (SharedPreferences,
    como já é feito em `SettingsPrefs`), restaurar ao iniciar e, em `MainActivity.onCreate`,
    voltar para o `LoginActivity` se `getLoggedInTechnician()` for `null`.

- [ ] **3. E-mail com espaço no final é rejeitado.**
  O autocompletar do teclado costuma adicionar esse espaço, e o login responde
  "E-mail ou senha inválidos".
  - Causa: o espaço só é ignorado na verificação de campo vazio (`LoginViewModel.login`),
    não no login em si (`MockAuthRepository.login` usa `email.toLowerCase()` sem `trim()`).
  - Correção sugerida: usar `email.trim().toLowerCase()` em `MockAuthRepository.login`
    (ou passar `current.getEmail().trim()` no `LoginViewModel`).

### Menores

- [ ] **4.** Tocar de novo no filtro já selecionado tira o destaque dele, mas a lista continua
  filtrada. Correção: adicionar `app:selectionRequired="true"` no `ChipGroup` em
  `app/src/main/res/layout/fragment_tickets.xml` (linha 35).
- [ ] **5.** As opções de Notificações são salvas (`SettingsPrefs`), mas nada no app usa
  essas preferências.
- [ ] **6.** Os botões "Finalizar atendimento" e "Sair" agem direto, sem pedir confirmação.
- [ ] **7.** A tela Sobre mostra "Versão 2.0_build0.1" (`strings.xml`, `about_message`),
  mas a versão no `app/build.gradle.kts` é `1.0`.
- [ ] **8.** Os diálogos misturam idiomas: aparece "Cancel" (`android.R.string.cancel`, que
  segue o idioma do aparelho) ao lado de "Salvar". Correção: usar uma string própria
  `action_cancel` = "Cancelar".
- [ ] **9.** Na barra inferior, o nome da aba ativa ("Chamados", "Início") fica sobreposto
  ao ícone.
- [ ] **10.** Os campos Status ("Disponível") e Cargo são fixos no layout. Os dois usuários
  de teste também veem os mesmos chamados.

## Problemas do emulador (não do app)

- Houve 2 fechamentos inesperados ao abrir telas
  (`IllegalArgumentException: Activity client record must not be null`). Nenhuma linha do
  erro aponta para código do app, e não se repetiram em mais 7 tentativas.
- Depois o emulador travou com "Process system isn't responding" e precisou ser reiniciado.
- Ele roda uma versão de testes do Android 17 (`CP41…dev-keys`). Vale confirmar num
  emulador com versão estável (API 35 ou 36).
