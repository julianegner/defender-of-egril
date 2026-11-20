// Block scope: all karma.config.d files share one generated scope.
{
    // Mocha's default 2000ms is too short for multi-turn simulations (e.g. the witches swamp
    // enemy-turn test) when run in the browser.
    config.client = config.client || {};
    config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 60000 });
}
