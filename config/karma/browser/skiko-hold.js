// Holds Karma's start until skiko-ready.mjs says Skiko's functions are the page's (skiko-on-js.js).
(function () {
    var karma = window.__karma__;
    var start = karma.loaded.bind(karma);
    var ready = new Promise(function (resolve) { window.formaticoSkikoReady = resolve; });
    karma.loaded = function () { ready.then(start); };
})();
