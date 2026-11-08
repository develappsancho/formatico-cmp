// The app's own words are Compose resources, which the test page fetches from composeResources/ beside it: Karma
// serves the test package's copy there (Pantrix's testing guide, 3.8, "The app's own words").
config.files.push({ pattern: "kotlin/composeResources/**/*", included: false, served: true, watched: false });
config.proxies["/composeResources/"] = "/base/kotlin/composeResources/";
