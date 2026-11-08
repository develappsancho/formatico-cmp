// A UI test waits for the app's own words, which the browser fetches after the screen's first frame: Mocha's two
// seconds for a test end before they come (PANTRIX_FINDINGS, "Test rehberi tarayıcıda UI testinin iki şartını
// söylemiyor"). A test that draws every case of a screen's previews runs for minutes on JavaScript, and Karma takes a
// browser quiet for 30 seconds for one that is gone.
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 600000 });
config.browserNoActivityTimeout = 600000;
