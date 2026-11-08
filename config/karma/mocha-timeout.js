// A UI test waits for the app's own words, which the browser fetches after the screen's first frame: Mocha's two
// seconds for a test end before they come (PANTRIX_FINDINGS, "Test rehberi tarayıcıda UI testinin iki şartını
// söylemiyor").
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 30000 });
