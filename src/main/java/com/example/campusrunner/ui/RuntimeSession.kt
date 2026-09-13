package com.example.campusrunner.ui

/**
 * What kind of location simulation is running.
 */
enum class RuntimeSessionKind { POINT, ROUTE }

/**
 * Lightweight description of the currently running simulation session.
 *
 * It only describes "what is being simulated" (point / route, coordinates,
 * route name, configured speed, mode, loop count, known total distance).
 *
 * It is NOT authoritative runtime state: whether the UI shows running/paused
 * always comes from the service chain (MockLocationService ->
 * MainActivity.refreshState() -> isRunning / isPaused). This session object is
 * created when a simulation starts, kept across pause/resume, and reconstructed
 * from the foreground service's persisted session when the activity returns.
 */
data class RuntimeSession(
    val kind: RuntimeSessionKind,
    val pointLat: Double? = null,
    val pointLng: Double? = null,
    val routeId: String? = null,
    val routeName: String? = null,
    val speedMps: Double? = null,
    val closeLoop: Boolean = false,
    val loopCount: Int = 1,
    val totalDistanceMeters: Double? = null,
    /** Active route clock (paused time excluded), persisted by the service. */
    val activeElapsedMillis: Long = 0L
)

/**
 * Normalizes a route name for runtime UI display:
 * null / blank / a lone "-" (ASCII or full-width) fall back to "路线模拟".
 * Real route names are kept as-is. Does not touch SavedRoute data.
 */
fun routeDisplayName(routeName: String?): String {
    val trimmed = routeName?.trim()
    return if (trimmed.isNullOrEmpty() || trimmed == "-" || trimmed == "－") "路线模拟" else trimmed
}

/**
 * Compact one-line runtime summary for non-home surfaces (e.g. map pages).
 * Returns null when nothing is running; otherwise
 * "模拟运行中 · 滨江路线" / "模拟暂停 · 定点模拟" / "模拟运行中 · 当前会话".
 */
fun runtimeSummary(isRunning: Boolean, isPaused: Boolean, session: RuntimeSession?): String? {
    if (!isRunning && !isPaused) return null
    val status = if (isPaused) "模拟暂停" else "模拟运行中"
    val what = when (session?.kind) {
        RuntimeSessionKind.ROUTE -> routeDisplayName(session.routeName)
        RuntimeSessionKind.POINT -> "定点模拟"
        null -> "当前会话"
    }
    return "$status · $what"
}
