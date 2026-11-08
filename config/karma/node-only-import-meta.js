// kotlinx-io, which the texts' encryption brings (cryptography-kotlin), reads `import.meta` bare in its Node helper
// (`kotlinx.io.node.getRequire`). Webpack leaves it so in a script the page loads as a classic one, where it is a
// syntax error and the app does not start. A browser never calls the helper: the bare `import.meta` is made an empty
// object, and `import.meta.url`, which webpack has written already, is left. The same as
// config/webpack/node-only-import-meta.js, which does it for the app, here for the pages Karma runs the tests in.
class NodeOnlyImportMeta {
    apply(compiler) {
        const { Compilation, sources } = compiler.webpack;
        compiler.hooks.thisCompilation.tap("NodeOnlyImportMeta", (compilation) => {
            compilation.hooks.processAssets.tap(
                { name: "NodeOnlyImportMeta", stage: Compilation.PROCESS_ASSETS_STAGE_SUMMARIZE },
                (assets) => {
                    for (const name of Object.keys(assets)) {
                        if (!/\.m?js$/.test(name)) continue;
                        const text = assets[name].source().toString();
                        const replaced = text.replace(/\bimport\.meta\b(?!\s*\.)/g, "({})");
                        if (replaced !== text) compilation.updateAsset(name, new sources.RawSource(replaced));
                    }
                }
            );
        });
    }
}
config.webpack.plugins.push(new NodeOnlyImportMeta());
