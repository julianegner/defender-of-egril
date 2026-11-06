// Disables analytics reporting in wasm browser tests so test runs don't send events
// to the production backend. Loaded before the test bundle; read by AnalyticsService.wasmJs.kt.
const path = require('path');

config.files = config.files || [];
config.files.unshift({
    pattern: path.join(__dirname, 'analytics-disabled-flag.js'),
    included: true,
    watched: false,
});
