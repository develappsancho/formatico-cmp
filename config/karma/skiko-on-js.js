// On JavaScript, Compose draws with Skiko's functions as the page's own: js-reexport-symbols.mjs puts them there from
// skiko.mjs, whose wasm loads after the page, as Compose's page does for the app. A UI test draws only once they are
// there: the tests start when Skiko is ready. The Wasm build has its own, and its tests are left as they are. The
// page's own scripts are in browser/, as every .js here is a part of Karma's configuration.
const fs = require("fs");
const path = require("path");
const kotlin = path.join(config.basePath, "kotlin");
// This file is copied into the test package's karma.conf.js: the page's scripts are found from the repository's root.
const browser = path.resolve(config.basePath, "..", "..", "..", "..", "config", "karma", "browser");
if (config.basePath.includes(`${path.sep}build${path.sep}js${path.sep}`) && fs.existsSync(path.join(kotlin, "js-reexport-symbols.mjs"))) {
    config.files.unshift(
        { pattern: path.join(browser, "skiko-hold.js"), included: true, watched: false },
        { pattern: path.join(browser, "skiko-ready.mjs"), type: "module", included: true, watched: false }
    );
    config.files.push(
        { pattern: path.join(kotlin, "js-reexport-symbols.mjs"), included: false, served: true, watched: false },
        { pattern: path.join(kotlin, "skiko.mjs"), included: false, served: true, watched: false },
        { pattern: path.join(kotlin, "skiko.wasm"), included: false, served: true, watched: false }
    );
    config.mime = Object.assign({}, config.mime, { "application/wasm": ["wasm"], "text/javascript": ["mjs"] });
}
