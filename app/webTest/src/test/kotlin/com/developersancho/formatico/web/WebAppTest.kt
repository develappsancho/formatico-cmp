package com.developersancho.formatico.web

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.util.Base64
import java.util.zip.Deflater
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The web app as a browser runs it, from its distribution: the loader's choice of build, the tab's history and address
 * following the screens, the links that carry a document, what a reload keeps, the window's width and the reader's
 * language. Each test has a Chrome of its own, whose profile, and with it what the page kept, nothing else shares.
 */
class WebAppTest {
    private val dist = File(checkNotNull(System.getProperty("formatico.web.dist")) { "formatico.web.dist is not set" })
    private val headers = File(checkNotNull(System.getProperty("formatico.web.headers")) { "formatico.web.headers is not set" })
    private lateinit var server: DistServer

    @BeforeTest
    fun serve() {
        server = DistServer(dist, nginxHeadersOf(headers))
    }

    @AfterTest
    fun stop() {
        server.close()
    }

    @Test
    fun aBrowserWithWasmRunsTheWasmBuildWithNothingThrown() = browse { page, chrome ->
        page.open()
        page.awaitTag("Button_paste")
        page.awaitText("Paste anything")
        assertTrue(page.ranWasm)
        assertEquals("#start", page.fragment)
        // Compose's resource reader too, handed the Wasm memory the supported way (WasmMemory.wasmJs.kt).
        assertEquals(emptyList(), chrome.errors)
    }

    @Test
    fun startNamesTheTabAndADocumentGivesItBack() = browse { page, chrome ->
        page.open()
        page.awaitText("Paste anything")
        // Start's page names the browser's tab, the app's name after it; a document's screen leaves the page's own.
        chrome.awaitUntil("Start named the tab") {
            chrome.evaluate("document.title")?.jsonPrimitive?.content == "Paste anything · Formatico"
        }
        page.press("ListItem_sample_order")
        page.awaitText("Valid JSON")
        chrome.awaitUntil("the tab had its own name back") { chrome.evaluate("document.title")?.jsonPrimitive?.content == "Formatico" }
    }

    @Test
    fun aBrowserWithoutWasmsGarbageCollectionRunsTheJavaScriptBuild() = browse { page, chrome ->
        // The loader asks whether the browser takes a module with a struct type and a `try`: an older one does not.
        chrome.beforeEachPage("WebAssembly.validate = () => false;")
        page.open()
        page.awaitText("Paste anything")
        assertTrue(page.ranJavaScript)
        assertFalse(page.ranWasm)
        page.press("ListItem_sample_order")
        page.awaitText("Valid JSON")
        assertEquals(emptyList(), chrome.errors)
    }

    @Test
    fun thePageRunsUnderItsContentSecurityPolicy() = browse { page, chrome ->
        page.open()
        page.awaitText("Paste anything")
        // A script written into the page, as an injected one would be, does not run, and its refusal is reported.
        // (What DevTools evaluates itself, as `evaluate` does, the policy lets run: the script is the page's here.)
        chrome.evaluate(
            "document.body.appendChild(Object.assign(document.createElement('script'), { textContent: 'window.injected = true' }))"
        )
        chrome.awaitUntil("the refusal was reported") { chrome.errors.any { POLICY in it && "script-src" in it } }
        assertEquals("undefined", chrome.evaluate("typeof window.injected")?.jsonPrimitive?.content)
        chrome.errors.removeAll { POLICY in it && "script-src" in it }
    }

    @Test
    fun webKitRunsTheJavaScriptBuild() = browse { page, chrome ->
        // Safari, and every browser on iOS, whose Wasm GC the loader does not trust.
        chrome.send(
            "Emulation.setUserAgentOverride",
            buildJsonObject { put("userAgent", SAFARI) }
        )
        page.open()
        page.awaitText("Paste anything")
        assertTrue(page.ranJavaScript)
        assertFalse(page.ranWasm)
    }

    @Test
    fun theTabsHistoryAndAddressFollowTheScreens() = browse { page, _ ->
        page.open()
        page.press("ListItem_sample_order")
        page.awaitText("Valid JSON")
        assertTrue(page.fragment.startsWith("#document?id="), page.fragment)

        page.back()
        page.awaitText("Paste anything")
        assertEquals("#start", page.fragment)

        page.forward()
        page.awaitText("Valid JSON")
        assertTrue(page.fragment.startsWith("#document?id="), page.fragment)
    }

    @Test
    fun aLinkOpensTheDocumentItCarriesUnderItsName() = browse { page, _ ->
        page.open(link("{\"id\": \"ord_8842\", \"status\": \"shipped\"}\n", name = "order.json"))
        page.awaitText("Valid JSON")
        page.awaitText("order.json")
        page.awaitText("status: \"shipped\"")
    }

    @Test
    fun aLinkCutShortIsSaidAndOpensNothing() = browse { page, _ ->
        page.open("#d=eJyrVspMUbJSUEpKzSmyUiopTUoqUqoFAGG1CBs")
        // The snackbar's words are not in what the browser is given for a screen reader, only its close button
        // (PANTRIX_FINDINGS: "Web'de snackbar'ın iletisi ekran okuyucuya gitmiyor"); the common tests read them.
        page.awaitTag("Snackbar_link_broken")
        page.awaitText("Paste anything")
        assertEquals("#start", page.fragment)
    }

    @Test
    fun theDocumentsOpenAreThereAgainAfterAReload() = browse { page, _ ->
        page.open()
        page.press("ListItem_sample_deployment")
        page.awaitText("Valid YAML")
        // The open tabs are written a moment after they change.
        Thread.sleep(KEEP_MILLIS)

        page.reload()
        page.awaitText("Valid YAML")
        page.awaitText("deployment.yaml")
    }

    @Test
    fun aPhonesWidthDrawsThePhonesStart() = browse { page, _ ->
        page.resize(PHONE_WIDTH, PHONE_HEIGHT, mobile = true)
        page.open()
        // A phone lists two samples until all are asked for, and opens a file with a button of its own.
        page.awaitTag("Link_all_samples")
        page.awaitTag("Button_open_file")
        assertFalse(page.has("FileDrop_file"))

        page.resize(DESKTOP_WIDTH, DESKTOP_HEIGHT)
        page.awaitTag("FileDrop_file")
        assertFalse(page.has("Link_all_samples"))
    }

    @Test
    fun aReaderOfTurkishIsSpokenToInTurkish() = browse("--lang=tr", "--accept-lang=tr") { page, _ ->
        page.open()
        page.awaitText("Ne varsa yapıştır")
        page.press("ListItem_sample_order")
        page.awaitText("Geçerli JSON")
    }

    /**
     * Runs [test] in a Chrome of its own, started with [flags], on a page of Formatico served from its distribution with
     * nginx's headers; and fails it where the page did anything its Content-Security-Policy refuses.
     */
    private fun browse(vararg flags: String, test: (FormaticoPage, Chrome) -> Unit) = Chrome(*flags).use { chrome ->
        chrome.beforeEachPage(VIOLATIONS)
        test(FormaticoPage(chrome, server.address), chrome)
        assertEquals(emptyList(), chrome.errors.filter { POLICY in it }, "What the Content-Security-Policy refused")
    }

    private companion object {
        /** What a test's errors say of what the policy refused. */
        const val POLICY = "Content-Security-Policy refused"

        /** Writes each thing the policy refuses, or only reports, as an error, before anything else on the page runs. */
        const val VIOLATIONS = "document.addEventListener('securitypolicyviolation', e => console.error('$POLICY ' + " +
            "e.disposition + ' ' + e.effectiveDirective + ': ' + (e.blockedURI || '') + ' ' + (e.sample || '')));"

        const val KEEP_MILLIS = 1_500L
        const val SAFARI = "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Version/27.2 Safari/605.1.15"
        const val PHONE_WIDTH = 390
        const val PHONE_HEIGHT = 844
        const val DESKTOP_WIDTH = 1440
        const val DESKTOP_HEIGHT = 900

        /** The fragment of a link that carries [text] as Formatico writes one: zlib's deflate, in Base64 for addresses. */
        fun link(text: String, name: String): String {
            val deflater = Deflater()
            deflater.setInput(text.encodeToByteArray())
            deflater.finish()
            val buffer = ByteArray(text.length * 2 + 64)
            val size = deflater.deflate(buffer)
            deflater.end()
            val packed = Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.copyOf(size))
            return "#d=$packed&n=$name"
        }
    }
}
