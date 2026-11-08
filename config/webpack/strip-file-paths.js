// Webpack writes a script's import.meta.url as the path the script had on the machine that built it
// (file:///Users/…/kotlin/skiko.mjs). The page never fetches it: it is cut to the file's own name, so that no
// machine's folders and no user's name go out with the app (Pantrix's web compatibility guide, 3.3).
class StripFilePaths {
    apply(compiler) {
        const { Compilation, sources } = compiler.webpack;
        compiler.hooks.thisCompilation.tap("StripFilePaths", (compilation) => {
            compilation.hooks.processAssets.tap(
                { name: "StripFilePaths", stage: Compilation.PROCESS_ASSETS_STAGE_SUMMARIZE },
                (assets) => {
                    for (const name of Object.keys(assets)) {
                        if (!/\.m?js$/.test(name)) continue;
                        const text = assets[name].source().toString();
                        const stripped = text.replace(/file:\/\/\/[^"'`\s]*\/([^/"'`\s]+)/g, "file:///$1");
                        if (stripped !== text) compilation.updateAsset(name, new sources.RawSource(stripped));
                    }
                }
            );
        });
    }
}
config.plugins.push(new StripFilePaths());
