package com.inklazy.routeverge.nfc

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedNfcUrlTest {
    @Test fun acceptsNormalAlipayLinks() {
        assertTrue(TrustedNfcUrl.accepts("https://render.alipay.com/p/s/ulink/qd?s=dc"))
        assertTrue(TrustedNfcUrl.accepts("https://alipay.com/path"))
        assertTrue(TrustedNfcUrl.accepts("alipay://nfc/app?id=20002153"))
    }

    @Test fun rejectsUntrustedSchemesHostsAndDeceptiveUrls() {
        listOf("https://evilalipay.com/path", "https://alipay.com.evil.test/",
            "https://alipay.com@evil.test/", "http://render.alipay.com/path",
            "javascript:alert(1)", "alipay://other/app?id=1", "alipay://nfc/other?id=1",
            "alipay://nfc/app", "alipay://nfc/app?t=foo", "alipay://nfc/app?id=bad",
            "https://render.alipay.com:444/path"
        ).forEach { assertFalse(it, TrustedNfcUrl.accepts(it)) }
    }
}
