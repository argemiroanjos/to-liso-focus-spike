# To Liso Focus Spike

## Visão geral

Este projeto é um spike técnico criado para validar uma hipótese muito específica:

> É possível detectar quando o usuário abre determinados aplicativos financeiros no Android e exibir um overlay de bloqueio de forma estável usando Accessibility Service?

A resposta curta é:

**Sim.**

O spike conseguiu validar:

* detecção de aplicativos em foreground
* exibição de overlay global
* bloqueio de interação
* debounce de eventos Android
* prevenção de loops e re-trigger
* controle por máquina de estados
* estabilidade em dispositivo físico Motorola (Moto G 5G)

---

# Objetivo do projeto

A ideia do projeto nasceu como uma prova de conceito para um possível “Modo Foco Financeiro”.

O comportamento esperado é:

1. O usuário abre um app monitorado
2. O Accessibility Service detecta a mudança de janela
3. Um overlay modal aparece bloqueando a interação
4. O usuário precisa decidir:

   * analisar o gasto
   * liberar o uso do aplicativo

O principal desafio técnico era validar se o Android permitiria isso de forma relativamente confiável.

---

# Tecnologias utilizadas

## Flutter

Responsável pela camada principal do aplicativo.

## Kotlin

Responsável pela integração Android nativa:

* AccessibilityService
* WindowManager
* Overlay global
* gerenciamento de estados

## Android Accessibility Service

Usado para detectar mudanças de foreground entre aplicativos.

## SYSTEM_ALERT_WINDOW

Permissão necessária para desenhar overlays sobre outros aplicativos.

---

# Estrutura principal do projeto

## `main.dart`

Entrada principal da aplicação Flutter.

---

## `FocusAccessibilityService.kt`

Coração do spike.

Responsável por:

* detectar mudanças de janela
* identificar apps monitorados
* controlar debounce
* evitar loops
* controlar máquina de estados
* acionar o overlay

---

## `OverlayManager.kt`

Responsável pela criação e remoção do overlay Android.

Utiliza:

* WindowManager
* TYPE_APPLICATION_OVERLAY
* LayoutInflater

---

## `AndroidManifest.xml`

Responsável por:

* registrar o AccessibilityService
* declarar permissões
* configurar integração Android

---

## `overlay_view.xml`

Layout visual do overlay exibido ao usuário.

---

# Fluxo técnico

## Fluxo simplificado

```text
Usuário abre app monitorado
        ↓
AccessibilityService detecta foreground
        ↓
Debounce estabiliza eventos
        ↓
Máquina de estados valida contexto
        ↓
Overlay exibido
        ↓
Usuário escolhe:
   • analisar
   • liberar
        ↓
Cooldown evita re-trigger imediato
```

---

# Principais problemas encontrados durante o spike

## 1. Eventos duplicados do Android

O Android dispara múltiplos eventos de mudança de janela em sequência.

Isso causava:

* overlays duplicados
* re-renderização constante
* loops

### Solução

Foi implementado debounce com `Handler + postAtTime`.

---

## 2. Launcher interrompendo o fluxo

Durante a abertura do overlay, o launcher aparecia temporariamente como foreground.

Isso fazia o overlay desaparecer imediatamente.

### Solução

Foi criada uma máquina de estados simples:

```text
IDLE
OVERLAY_ACTIVE
```

Enquanto o overlay está ativo, transições de janela são ignoradas.

---

## 3. Loop infinito do próprio app

O próprio aplicativo Flutter estava gerando eventos de foreground.

Isso criava:

```text
overlay → app → overlay → app → overlay...
```

### Solução

Pacotes internos passaram a ser ignorados:

```kotlin
private val ignoredPackages = setOf(
    "com.example.to_liso_focus_spike",
    "com.android.systemui",
)
```

---

## 4. Re-trigger imediato após liberar o overlay

Ao fechar o overlay, o Android disparava novamente o mesmo evento.

### Solução

Foi implementado cooldown contextual.

---

# Estado atual do spike

## Validado

* Accessibility Service
* Overlay global
* Controle de estados
* Debounce
* Cooldown
* Bloqueio modal
* Fluxo de foreground

---

## Não implementado ainda

* banco de dados
* analytics
* navegação Flutter integrada
* platform channel completo
* persistência
* regras financeiras reais
* onboarding
* autenticação

---

# Como executar o projeto

## Pré-requisitos

Instale:

* Flutter SDK
* Android Studio
* Android SDK
* dispositivo Android físico

> Recomenda-se testar em dispositivo físico.
> Emuladores podem apresentar comportamento inconsistente com AccessibilityService.

---

# 1. Clonar o projeto

```bash
git clone git@github.com:argemiroanjos/to-liso-focus-spike.git
```

---

# 2. Entrar na pasta

```bash
cd to-liso-focus-spike
```

---

# 3. Instalar dependências

```bash
flutter pub get
```

---

# 4. Executar o projeto

```bash
flutter run
```

---

# 5. Conceder permissão de overlay

No Android:

```text
Configurações
→ Apps
→ Acesso especial
→ Exibir sobre outros apps
```

Habilite para o aplicativo.

---

# 6. Ativar Accessibility Service

No Android:

```text
Configurações
→ Acessibilidade
→ Serviços instalados
→ To Liso Focus Spike
→ Ativar
```

---

# 7. Testar

Abra um aplicativo monitorado:

```text
com.google.android.calculator
```

O overlay deve aparecer.

---

# Logs úteis

Para acompanhar os eventos em tempo real:

## Linux / macOS

```bash
adb logcat -s FocusService:D OverlayManager:D
```

---

## Windows (caso o adb não esteja no PATH)

Em muitos casos o Flutter reconhece o dispositivo normalmente, mas o comando `adb` não funciona diretamente no terminal.

Isso acontece porque o `platform-tools` não está configurado no PATH do Windows.

### Como descobrir o caminho do SDK Android

Execute:

```bash
flutter doctor -v
```

Procure uma linha parecida com:

```text
Android SDK at C:\Users\SEU_USUARIO\AppData\Local\Android\sdk
```

Depois entre na pasta:

```text
platform-tools
```

Lá estará o arquivo:

```text
adb.exe
```

---

## Executando logcat diretamente pelo caminho completo

Exemplo:

```bash
"C:\Users\SEU_USUARIO\AppData\Local\Android\sdk\platform-tools\adb.exe" logcat -s FocusService:D OverlayManager:D
```

Substitua pelo caminho correto da sua máquina.

---

## Exemplo real

```bash
"C:\Users\VAIO\AppData\Local\Android\sdk\platform-tools\adb.exe" logcat -s FocusService:D OverlayManager:D
```

Isso evita problemas de PATH e já permite acompanhar os logs imediatamente.

---

# Apps monitorados atualmente

```kotlin
private val monitoredApps = setOf(
    "com.google.android.calculator",
    "com.nu.production",
    "br.com.intermedium",
)
```

---

# Arquitetura usada

O spike utiliza uma abordagem híbrida:

```text
Flutter
  ↓
Platform Layer Android
  ↓
AccessibilityService + Overlay
```

A parte crítica do sistema roda em Kotlin nativo.

---

# Próximos passos possíveis

## Integração Flutter ↔ Kotlin

Adicionar Platform Channels para:

* abrir telas Flutter a partir do overlay
* enviar eventos do Android para Flutter
* iniciar fluxo de análise financeira

---

## Persistência

Adicionar:

* Hive
* Drift
* SQLite

---

## Regras reais de foco financeiro

Exemplos:

* tempo máximo por app
* limite diário
* bloqueio contextual
* análise de intenção de gasto

---

# Observações importantes

Este projeto é um spike técnico.

O objetivo principal nunca foi:

* arquitetura final
* design final
* UX final
* código enterprise

O objetivo foi:

```text
reduzir risco técnico
```

E isso foi alcançado.

---

# Resultado final do spike

## Hipótese validada

O Android permite detectar aplicativos em foreground e bloquear interação usando AccessibilityService + Overlay global de forma estável.

---

# Autor
Argemiro dos Anjos

Projeto desenvolvido como spike técnico para validação de um possível produto de foco financeiro.
