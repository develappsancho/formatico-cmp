// Chooses the build this browser can run: Formatico's Wasm build where it has Wasm GC and exceptions and is not
// WebKit, its JavaScript build otherwise, or when the Wasm build fails with a Wasm error before its first frame.
// After Pantrix UI's guide "Old Browsers and a Wasm That Will Not Start, on the Web", section 5.2.
(function () {
    "use strict";
    // The module of Compose's loader: a struct type and a `try`. A browser that takes it has both GC and exceptions.
    var FEATURES = new Uint8Array([
        0, 97, 115, 109, 1, 0, 0, 0, 1, 8, 2, 95, 1, 120, 0, 96, 0, 0, 3, 3, 2, 1, 1, 10, 14, 2, 6, 0, 6, 64, 25, 11,
        11, 5, 0, 208, 112, 26, 11, 0, 45, 4, 110, 97, 109, 101, 1, 15, 2, 0, 5, 102, 117, 110, 99, 48, 1, 5, 102, 117,
        110, 99, 49, 4, 8, 1, 0, 5, 116, 121, 112, 101, 48, 10, 11, 1, 0, 1, 0, 6, 102, 105, 101, 108, 100, 48
    ]);
    var WASM = "webApp-wasm.js", JS = "webApp-js.js";
    var starting = null;

    function drawn() { return !document.getElementById("loading"); }
    function load(src, onFailure) {
        var script = document.createElement("script");
        script.src = src;
        script.onerror = onFailure;
        document.body.appendChild(script);
    }
    // Neither build can open: the loading mark gives way to a note.
    function showUnsupported() {
        starting = null;
        var spinner = document.querySelector("#loading .spinner");
        if (spinner) spinner.remove();
        var note = document.getElementById("unsupported");
        if (note) note.hidden = false;
    }
    function startJavaScript() {
        starting = "js";
        var box = document.getElementById("app");
        while (box && box.firstChild) box.removeChild(box.firstChild);
        load(JS, showUnsupported);
    }
    function wasmFailed(error) {
        var wasm = typeof WebAssembly === "object";
        return error instanceof RangeError || wasm && (error instanceof WebAssembly.CompileError ||
            error instanceof WebAssembly.LinkError || error instanceof WebAssembly.RuntimeError);
    }
    function thrownBy(error, script) { return String(error && error.stack || "").indexOf(script) >= 0; }
    // WebKit: Safari and every browser on iOS. Those built on Chrome name Chrome in their user agent as well.
    function webKit() {
        var agent = typeof navigator === "object" && navigator.userAgent || "";
        return /AppleWebKit\//.test(agent) && !/Chrom(e|ium)\//.test(agent);
    }
    function failed(error) {
        if (starting === null || drawn() || !wasmFailed(error)) return;
        if (starting === "wasm") startJavaScript();
        else if (!thrownBy(error, WASM)) showUnsupported();
    }

    window.addEventListener("error", function (event) { failed(event.error); });
    window.addEventListener("unhandledrejection", function (event) { failed(event.reason); });

    var collected = false;
    try { collected = typeof WebAssembly === "object" && WebAssembly.validate(FEATURES); } catch (e) {}
    if (collected && !webKit()) { starting = "wasm"; load(WASM, startJavaScript); }
    else if (typeof WebAssembly === "object") startJavaScript();
    else showUnsupported();
})();
