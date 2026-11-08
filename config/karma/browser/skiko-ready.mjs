// Makes Skiko's functions the page's and waits for its wasm, then lets Karma start the tests (skiko-on-js.js).
import { api } from "/base/kotlin/js-reexport-symbols.mjs";

await api.awaitSkiko;
window.formaticoSkikoReady();
