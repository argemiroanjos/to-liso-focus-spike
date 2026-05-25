package com.example.to_liso_focus_spike

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService : AccessibilityService() {

    private val tag = "FocusService"

    // --- Máquina de estados ---
    private enum class FocusState {
        IDLE,           // monitorando, sem overlay
        OVERLAY_ACTIVE  // overlay visível - ignora transições de janela
    }

    private var state = FocusState.IDLE

    // --- Debounce ---
    // Transições de janela disparam múltiplos eventos em rápida sequência.
    // 300ms é suficiente para aguardar a janela "estabilizar" sem impacto
    // perceptível ao usuário.
    private val handler = Handler(Looper.getMainLooper())
    private val debounceDelayMs = 300L

    //  --- Cooldown contextual ---
// lastDismissedPackage: qual app estava em foreground quando o overlay foi dispensado
// lastReleaseTime: quando foi dispensado
// Invariante: cooldown só se aplica se o mesmo app ainda estiver em foreground.
// Quando outro app aparece, o cooldown é resetado - o retorno ao app monitorado
// é tratado como uma nova sessão.
private var lastDismissedPackage: String? = null
private var lastReleaseTime = 0L
private val releaseCooldownMs = 3_000L // 3s é suficiente para evitar re-trigger acidental

    // --- Apps monitorados ---
    private val monitoredApps = setOf(
        "com.google.android.calculator", // para teste sem banco instalado
        "com.nu.production",             // Nubank
        "br.com.intermedium",            // PicPay
    )

    // --- Apps que nunca devem disparar lógica ---
    // O próprio app + UI do sistema. Launcher não entra aqui intencionalmente
    // - será tratado pela máquina de estados.
    private val ignoredPackages = setOf(
        "com.example.to_liso_focus_spike",
        "com.android.systemui",
    )

    private lateinit var overlayManager: OverlayManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayManager(this)
        Log.d(tag, "Serviço conectado — estado: IDLE")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Filtro de tipo - única linha que elimina 90% dos bugs de loop
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return

        // Ignora pacotes do sistema e do próprio app
        if (pkg in ignoredPackages) return

        Log.d(tag, "Evento: $pkg | estado: $state")

        // Debounce - cancela qualquer processamento pendente e agenda novo
        handler.removeCallbacksAndMessages(DEBOUNCE_TOKEN)
        handler.postAtTime(
            { processWindowChange(pkg) },
            DEBOUNCE_TOKEN,
            android.os.SystemClock.uptimeMillis() + debounceDelayMs
        )
    }

    private fun processWindowChange(pkg: String) {
    // Overlay ativo -> ignora tudo
    if (state == FocusState.OVERLAY_ACTIVE) {
        Log.d(tag, "Overlay ativo — transição ignorada: $pkg")
        return
    }

    // --- Reset contextual do cooldown ---
    // Se o app que estava em cooldown saiu do foreground, o cooldown é cancelado.
    // Quando ele voltar, será tratado como nova sessão.
    if (lastDismissedPackage != null && pkg != lastDismissedPackage) {
        Log.d(tag, "App monitorado saiu do foreground — cooldown resetado")
        lastDismissedPackage = null
        lastReleaseTime = 0L
    }

    // --- Verifica cooldown ---
    // Só aplica se for o MESMO app que foi dispensado e ainda dentro da janela
    val now = System.currentTimeMillis()
    if (pkg == lastDismissedPackage && now - lastReleaseTime < releaseCooldownMs) {
        Log.d(tag, "Cooldown ativo — ignorando: $pkg")
        return
    }

    // --- Detecta app monitorado ---
    if (pkg in monitoredApps) {
        Log.d(tag, "App monitorado detectado: $pkg — exibindo overlay")
        state = FocusState.OVERLAY_ACTIVE

        overlayManager.show(
            packageName = pkg,
            onAnalyze = {
                Log.d(tag, "Usuário escolheu analisar — source=block_overlay")
                lastDismissedPackage = pkg
                lastReleaseTime = System.currentTimeMillis()
                state = FocusState.IDLE
            },
            onRelease = {
                Log.d(tag, "Usuário liberou o gasto")
                lastDismissedPackage = pkg
                lastReleaseTime = System.currentTimeMillis()
                state = FocusState.IDLE
            }
        )
    }
}

    override fun onInterrupt() {
        Log.d(tag, "Serviço interrompido")
        resetState()
    }

    override fun onDestroy() {
        super.onDestroy()
        resetState()
        handler.removeCallbacksAndMessages(DEBOUNCE_TOKEN)
        Log.d(tag, "Serviço destruído")
    }

    private fun resetState() {
        state = FocusState.IDLE
        overlayManager.hide()
    }

    companion object {
        // Token único para o debounce — evita conflito com outros postAtTime
        private val DEBOUNCE_TOKEN = Object()
    }
}