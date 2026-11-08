package com.developersancho.formatico.web

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Formatico in [chrome]'s page, served at [address], read as a screen reader reads it. Compose draws on a canvas and
 * mirrors what it draws, for a screen reader, into elements of a root of its own inside the canvas's shadow root; a
 * Pantrix component's automation tag is its element's `id` there (`Button_paste`), and its name its `aria-label` or
 * its text. A press is the pointer's, at the middle of the element, as a reader's mouse presses it.
 */
class FormaticoPage(private val chrome: Chrome, private val address: String) {
    /** Opens the app at [fragment], `#d=…` for a shared link, and waits until it draws. */
    fun open(fragment: String = "") {
        chrome.send("Page.navigate", buildJsonObject { put("url", address + fragment) })
        chrome.awaitUntil("Formatico drew") { chrome.evaluate("$ROOT !== null")?.jsonPrimitive?.boolean == true }
    }

    /** Opens the page again, as the browser's reload does, and waits until it draws. */
    fun reload() {
        chrome.send("Page.reload")
        chrome.awaitUntil("Formatico drew again") { chrome.evaluate("$ROOT !== null")?.jsonPrimitive?.boolean == true }
    }

    /** Tells the page its window is [width] by [height], and a phone's, which is used by touch, where [mobile] says so. */
    fun resize(width: Int, height: Int, mobile: Boolean = false) {
        chrome.send(
            "Emulation.setDeviceMetricsOverride",
            buildJsonObject {
                put("width", width)
                put("height", height)
                put("deviceScaleFactor", 1)
                put("mobile", mobile)
            }
        )
    }

    /** Whether the component tagged [tag] is drawn, in the room the window gives it. */
    fun has(tag: String): Boolean = box(tag) != null

    /** Whether a text holding [text] is drawn, as a component's name or its words. */
    fun shows(text: String): Boolean = chrome.evaluate(
        """(() => { const root = $ROOT; if (!root) return false;
            return [...root.querySelectorAll('*')].some(e => {
              const r = e.getBoundingClientRect();
              return r.width > 0 && ((e.getAttribute('aria-label') || '').includes(${quoted(text)}) ||
                [...e.childNodes].some(n => n.nodeType === 3 && n.textContent.includes(${quoted(text)})));
            }); })()"""
    )?.jsonPrimitive?.boolean == true

    /** Waits until the component tagged [tag] is drawn. */
    fun awaitTag(tag: String) = chrome.awaitUntil("$tag was drawn") { has(tag) }

    /** Waits until a text holding [text] is drawn. */
    fun awaitText(text: String) = chrome.awaitUntil("\"$text\" was drawn") { shows(text) }

    /** Presses the component tagged [tag] once it is drawn, with the pointer, at its middle. */
    fun press(tag: String) {
        awaitTag(tag)
        val box = checkNotNull(box(tag)) { "$tag is not drawn" }
        val x = box.getValue("x").jsonPrimitive.double + box.getValue("width").jsonPrimitive.double / 2
        val y = box.getValue("y").jsonPrimitive.double + box.getValue("height").jsonPrimitive.double / 2
        mouse("mouseMoved", x, y)
        mouse("mousePressed", x, y)
        mouse("mouseReleased", x, y)
    }

    /** The address's fragment: `#start`, `#document?id=1`. */
    val fragment: String get() = chrome.evaluate("location.hash")?.jsonPrimitive?.content.orEmpty()

    /** Goes back in the tab's history, as the browser's back button does. */
    fun back() {
        chrome.evaluate("history.back()")
    }

    /** Goes forward in the tab's history. */
    fun forward() {
        chrome.evaluate("history.forward()")
    }

    /**
     * Which of the two builds the loader started, by the script it loaded: `webApp-wasm.js` or `webApp-js.js`. Both
     * fetch a Wasm module, Skia's, which the JavaScript build draws with too.
     */
    val ranWasm: Boolean
        get() = chrome.evaluate("performance.getEntriesByType('resource').some(e => e.name.endsWith('/webApp-wasm.js'))")
            ?.jsonPrimitive?.boolean == true

    /** Whether the loader started the JavaScript build. */
    val ranJavaScript: Boolean
        get() = chrome.evaluate("performance.getEntriesByType('resource').some(e => e.name.endsWith('/webApp-js.js'))")
            ?.jsonPrimitive?.boolean == true

    private fun box(tag: String): JsonObject? {
        val value = chrome.evaluate(
            """(() => { const root = $ROOT; const e = root && root.querySelector('[id=' + CSS.escape(${quoted(tag)}) + ']');
                if (!e) return null; const r = e.getBoundingClientRect();
                return r.width > 0 && r.height > 0 ? { x: r.x, y: r.y, width: r.width, height: r.height } : null; })()"""
        )
        return if (value == null || value is JsonNull) null else value.jsonObject
    }

    private fun mouse(type: String, x: Double, y: Double) {
        chrome.send(
            "Input.dispatchMouseEvent",
            buildJsonObject {
                put("type", type)
                put("x", x)
                put("y", y)
                put("button", if (type == "mouseMoved") "none" else "left")
                put("clickCount", if (type == "mouseMoved") 0 else 1)
            }
        )
    }

    private companion object {
        /** Compose's root for a screen reader, inside the shadow root of the element that holds the canvas. */
        const val ROOT = "([...document.querySelectorAll('*')].map(e => e.shadowRoot && " +
            "e.shadowRoot.getElementById('cmp_a11y_root')).find(r => r) || null)"

        fun quoted(text: String): String = kotlinx.serialization.json.JsonPrimitive(text).toString()
    }
}
