# TUIpeRacer2 — Arquitetura do Sistema

## Visão Geral

TUIpeRacer2 é um jogo de corrida de digitação em Java 21 + JavaFX 21. O jogador (ou jogadores em rede) digita um texto o mais rápido possível; o placar final exibe WPM e precisão. O projeto é um trabalho final de OOP e por isso exige: hierarquia de classes abstrata, 2+ interfaces, 6+ exceções, coleção polimórfica e persistência em arquivo.

**Stack:** Java 21 · JavaFX 21.0.2 · Maven

---

## Estrutura de Pacotes

```
com.tuiperacer/
├── Main.java               — entrada da aplicação e navegação
├── AppContext.java          — singleton de estado global
├── exception/              — exceções customizadas
├── interfaces/             — contratos Persistable e Typeable
├── model/                  — entidades do domínio
├── service/                — lógica de negócio
├── persistence/            — leitura e escrita em arquivo
├── network/                — servidor e cliente TCP
└── view/                   — controllers JavaFX
```

---

## Camadas e Responsabilidades

```
┌──────────────────────────────────────────┐
│              View (JavaFX)               │  Controllers + FXML
├──────────────────────────────────────────┤
│             Service Layer                │  RaceService, LobbyService
├──────────────────────────────────────────┤
│           Model / Domain                 │  Race, Player hierarchy
├────────────────────┬─────────────────────┤
│    Persistence     │      Network        │  Arquivo ↔ TCP
└────────────────────┴─────────────────────┘
          AppContext une todas as camadas
```

---

## Pacote Raiz

### `Main`
Ponto de entrada da aplicação. Mantém referência estática ao `Stage` principal e expõe `navigateTo(String)`, que carrega o FXML correspondente e aplica o CSS.

Mapeamento de telas:

| Chave | FXML | Controller |
|---|---|---|
| `main_menu` | main_menu.fxml | MainMenuController |
| `setup` | setup.fxml | SetupController |
| `race` | race.fxml | RaceController |
| `online_lobby` | online_lobby.fxml | OnlineLobbyController |
| `results` | results.fxml | ResultsController |

### `AppContext` — Singleton
Estado global compartilhado entre todos os controllers e a camada de rede.

```
AppContext
  ├── RaceService        — corrida em andamento
  ├── LobbyService       — entradas de lobby LAN
  ├── RaceResultManager  — histórico e persistência
  ├── RaceResult         — resultado da última corrida
  ├── RaceServer?        — não-nulo se este processo é host
  └── RaceClient?        — não-nulo se este processo é cliente
```

`resetForNewRace()` para o servidor/cliente ativos e recria o `RaceService` para a próxima partida.

---

## Interfaces

### `Typeable`
Contrato de digitação implementado pela hierarquia `Player`.

| Método | Descrição |
|---|---|
| `processInput(char)` | Registra tecla; lança `InvalidInputException` se errada |
| `isFinished()` | `true` quando todo o texto foi digitado |
| `getProgress()` | 0.0 – 1.0 |
| `getWPM()` | Palavras por minuto (chars corretos ÷ 5 ÷ minutos) |
| `getAccuracy()` | % de chars corretos sobre tentativas totais |

### `Persistable`
Contrato de serialização implementado por `RaceResult` e `RaceResultManager`.

| Método | Descrição |
|---|---|
| `serialize()` | Converte objeto em `String` |
| `deserialize(String)` | Popula objeto a partir de `String`; lança `FileLoadException` |

---

## Exceções Customizadas

| Classe | Tipo | Quando é lançada |
|---|---|---|
| `ConnectionException` | checked | Falha ao criar socket (servidor ou cliente) |
| `DuplicatePlayerException` | checked | Nome de jogador já existe na corrida |
| `FileLoadException` | checked | Erro de I/O em arquivo ou recurso embutido |
| `InvalidInputException` | unchecked | Tecla digitada não corresponde ao esperado |
| `InvalidTextException` | checked | Texto da corrida é nulo ou em branco |
| `PlayerNotFoundException` | checked | Jogador não encontrado por nome |
| `RaceAlreadyStartedException` | unchecked | Operação inválida em corrida já iniciada |

---

## Model

### Hierarquia de `Player` (coleção polimórfica)

```
Player  (abstract)
  implements Typeable
  ├── PlayerLocal   — jogador humano local; valida cada tecla
  ├── PlayerCpu     — bot; auto-digita com jitter de ±20% no WPM-alvo
  └── PlayerTcp     — espelho de jogador remoto; aceita chars recebidos via rede
```

`PlayerTcp` mantém um `playerId` (slot numérico) para roteamento das mensagens de rede.

### `Race`
Agrega o texto e a lista polimórfica `List<Player>`. Controla o ciclo de vida via `RaceState`:

```
WAITING ──start()──► RUNNING ──finish()──► FINISHED
```

`buildResult()` tira um snapshot imutável (`RaceResult`) do estado final.

### `RaceResult` / `PlayerResult`
Snapshots imutáveis dos resultados finais. Implementam `Persistable` para serialização em arquivo.

---

## Service

### `RaceService`
Delega para `Race` e expõe operações de alto nível:
- `createRace(text)` — cria nova `Race` com texto
- `addPlayer` / `removePlayer` / `editPlayerName`
- `startRace()` / `finishRace()`

### `LobbyService`
Lista de servidores descobertos na LAN (`List<LobbyEntry>`). Upsert por `host:port`.

---

## Persistence

### `TextFileLoader`
Utilitário estático. `loadBuiltinText(n)` embaralha as palavras de `/words/en.txt` e retorna `n` palavras. `loadFromFile(path)` lê qualquer arquivo de texto.

### `RaceResultManager`
Acumula `RaceResult`s em memória e lida com I/O de arquivo.

- `saveResult(path, result)` — salva **uma** corrida em formato legível:

```
  TUIpeRacer2 — Resultado da Corrida
  ────────────────────────────────────────────────────
  Data  : 2026-06-21 14:30:00
  Texto : "the quick brown fox jumps over the lazy..."
  ────────────────────────────────────────────────────
   #   Jogador              WPM   Precisão
  ────────────────────────────────────────────────────
   1   Alice              45.2    98.5%   ✓
   2   Bob [CPU]          42.1   100.0%   ✓
   3   Charlie             0.0     0.0%   ✗
  ────────────────────────────────────────────────────
```

- `saveToFile` / `loadFromFile` — serialização completa com marcadores `---BEGIN---/---END---` para múltiplas corridas.

---

## Network

### Protocolo (linhas UTF-8 terminadas em `\n`)

| Direção | Mensagem | Significado |
|---|---|---|
| Server → Client | `TEXT:<texto>` | Texto da corrida |
| Server → Client | `MYID:<n>` | Slot do jogador |
| Server → All | `JOIN:<n>:<nome>` | Jogador entrou no lobby |
| Server → All | `START` | Corrida começando |
| Server → All | `C:<n>:<codepoint>` | Jogador n digitou um char |
| Client → Server | `NAME:<nome>` | Primeira mensagem após conectar |
| Client → Server | `C:<codepoint>` | Jogador digitou um char |

Slot **0** é sempre o host; slots **1+** são clientes na ordem de conexão.

### `RaceServer`
Gerencia conexões, distribui IDs e faz broadcast das mensagens.

```
start()
  └── acceptLoop (executor thread)
        └── ClientHandler (1 thread por cliente)
              ├── envia TEXT + MYID + jogadores existentes
              ├── aguarda NAME do cliente
              ├── broadcastJoin para todos
              └── relay loop: C: → broadcastChar
```

Callbacks invocados no FX thread via `Platform.runLater`:
- `onPlayerJoined(name)` — atualiza lobby do host
- `onCharReceived(id, char)` — atualiza `PlayerTcp` no `RaceController`

### `RaceClient`
Conecta ao `RaceServer`, envia `NAME` e mantém um loop de leitura em thread própria.

Callbacks invocados no FX thread:
- `onTextReceived`, `onMyIdReceived`, `onPlayerJoined`, `onStartReceived`, `onCharReceived`, `onError`

Métodos de envio: `sendName(String)`, `sendChar(char)`.

---

## View / Controllers

### `MainMenuController`
Ponto de entrada visual. Botões navegam para `setup` (corrida local), `online_lobby` (host ou join) e `results`.

### `SetupController`
Permite adicionar/remover/renomear `PlayerLocal` e `PlayerCpu`, carregar texto de arquivo ou gerar aleatório, e iniciar a corrida.

### `OnlineLobbyController`

**Modo HOST:**
1. Tenta bind nas portas 3000–3010
2. Exibe IP:porta para o outro jogador
3. Aguarda conexões; lista jogadores via `onPlayerJoined`
4. "Iniciar Corrida": cria `PlayerLocal` (host) + `PlayerTcp` (cada cliente) → `broadcastStart()` → navega para race

**Modo JOIN:**
1. Usuário digita IP, porta e nome → conecta → envia `NAME`
2. Tela de espera: `onPlayerJoined` popula a lista de sala
3. `onStartReceived`: cria `PlayerLocal` (si mesmo) + `PlayerTcp` (demais) → navega para race

### `RaceController`
O coração do jogo. Roda um `AnimationTimer` a ~60 FPS.

```
initialize()
  ├── cria PlayerPane para cada jogador
  ├── registra onCharReceived no server/client → remoteCharQueue
  └── instala handlers de teclado na cena

onKeyTyped(char c)
  ├── PlayerLocal.processInput(c)
  ├── atualiza PlayerPane
  └── server.broadcastHostChar(c)  ou  client.sendChar(c)

onGameTick(now)
  ├── atualiza timer
  ├── drena remoteCharQueue → PlayerTcp.processInput + PlayerPane.update
  ├── PlayerCpu.autoTick → PlayerPane.update
  ├── updateStats em todos
  └── isAllFinished → finishRace()
```

**`PlayerPane`** (inner class): renderiza o texto em três segmentos — digitado (ciano), cursor (branco), restante (cinza) — usando `TextFlow` com nós `Text` estilizados via CSS.

### `ResultsController`
Exibe a tabela de resultados ordenada por posição (quem terminou primeiro, depois por WPM). Permite salvar o resultado em arquivo formatado.

---

## Fluxo Completo — Corrida Online

```
HOST                                     CLIENTE
────                                     ───────
OnlineLobbyController
  startServer() → porta 3000–3010
                                         OnlineLobbyController
                                           connect() → sendName()
                                         ← TEXT, MYID, JOIN (lobby existente)
broadcastJoin →                          ← JOIN (este cliente)
  lista atualizada                         tela de espera
  
"Iniciar Corrida"
  cria PlayerLocal + PlayerTcp[]
  raceService.startRace()
  broadcastStart() →                     ← START
  navega para race                         cria PlayerLocal + PlayerTcp[]
                                           navega para race

─── CORRIDA ───
onKeyTyped('h')                          
  processInput('h')                      
  broadcastHostChar('h') →              ← C:0:104
                                           remoteCharQueue ← (0, 'h')
                                           onGameTick: PlayerTcp[0].processInput
                                         onKeyTyped('e')
← C:1:101                                  processInput('e')
  remoteCharQueue ← (1, 'e')               sendChar('e') →
  PlayerTcp[1].processInput
```

---

## Decisões de Design Relevantes

| Decisão | Motivo |
|---|---|
| `Player` abstrato com `processInput` polimórfico | Permite tratar LOCAL, CPU e TCP uniformemente no game loop |
| `AppContext` singleton | Evita passar referências por todos os controllers; simplifica navegação entre telas |
| `AnimationTimer` como game loop | Integração natural com JavaFX; garante atualizações de UI no FX thread |
| `ConcurrentLinkedQueue` para chars remotos | Chars chegam em thread de rede; game loop drena no FX thread sem bloqueio |
| Protocolo baseado em linhas de texto | Simples de debugar; sem necessidade de framing binário para este tamanho de projeto |
| Tentar portas 3000–3010 | Evita falha quando a porta preferida está ocupada (ex: duas instâncias no mesmo PC) |
