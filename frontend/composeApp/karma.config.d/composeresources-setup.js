// Serves Compose resources (e.g. repository level/map JSON) to wasm browser tests.
// Without this, the browser requests /composeResources/... and Karma responds with 404,
// because the resources live under the test package's kotlin/composeResources directory.
// Block scope: all karma.config.d files share one generated scope, so avoid clashing top-level names.
{
    const resourcesDir = require('path').join(__dirname, 'kotlin', 'composeResources');

    config.files = config.files || [];
    config.files.push({
        pattern: require('path').join(resourcesDir, '**', '*'),
        included: false,
        served: true,
        watched: false,
    });

    config.proxies = config.proxies || {};
    config.proxies['/composeResources/'] = '/absolute' + resourcesDir.replace(/\\/g, '/') + '/';
}
