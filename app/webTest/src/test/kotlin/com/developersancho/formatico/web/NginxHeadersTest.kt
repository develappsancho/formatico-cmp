package com.developersancho.formatico.web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The headers the tests serve the app with: those nginx's `deploy/web/headers.conf` adds over plain HTTP. */
class NginxHeadersTest {
    @Test
    fun aHeaderIsReadQuotedOrNot() {
        val headers = nginxHeadersOf(
            listOf(
                "# A comment, and no header.",
                "add_header X-Content-Type-Options nosniff always;",
                """    add_header Content-Security-Policy "default-src 'none'; img-src 'self'" always;"""
            )
        )
        assertEquals(
            mapOf(
                "X-Content-Type-Options" to "nosniff",
                "Content-Security-Policy" to "default-src 'none'; img-src 'self'"
            ),
            headers
        )
    }

    @Test
    fun aHeaderNginxDecidesForEachRequestIsLeftOut() {
        val headers = nginxHeadersOf(
            listOf(
                "add_header Strict-Transport-Security \$formatico_hsts always;",
                "add_header Referrer-Policy no-referrer always;"
            )
        )
        assertEquals(mapOf("Referrer-Policy" to "no-referrer"), headers)
    }

    @Test
    fun theHostsHeadersOverPlainHttpHoldThePolicyAndNoHsts() {
        val headers = nginxHeadersOf(File(System.getProperty("formatico.web.headers")))
        assertTrue(headers.getValue("Content-Security-Policy").startsWith("default-src 'none'"))
        assertEquals("nosniff", headers["X-Content-Type-Options"])
        assertFalse("Strict-Transport-Security" in headers)
    }
}
