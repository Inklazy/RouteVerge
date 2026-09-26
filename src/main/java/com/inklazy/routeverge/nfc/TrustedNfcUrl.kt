package com.inklazy.routeverge.nfc

import java.net.URI

/** One trust rule for NFC discovery, storage and launch. */
internal object TrustedNfcUrl {
    fun accepts(value: String): Boolean {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return false
        if (uri.rawUserInfo != null || uri.fragment != null) return false
        return when (uri.scheme?.lowercase()) {
            "https" -> {
                val host = uri.host?.lowercase() ?: return false
                (host == "alipay.com" || host.endsWith(".alipay.com")) &&
                    (uri.port == -1 || uri.port == 443)
            }
            "alipay" -> uri.host.equals("nfc", ignoreCase = true) &&
                uri.path == "/app" &&
                uri.rawQuery.orEmpty().contains(Regex("""(?:^|&)id=\d+(?:&|$)"""))
            else -> false
        }
    }
}
