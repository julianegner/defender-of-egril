/**
 * Enables Single Page Application (SPA) routing for the Kotlin/Wasm app.
 *
 * When a route like /data-privacy/en is requested but doesn't exist as a static file,
 * the dev server falls back to serving index.html, which loads the Kotlin/Wasm app.
 * The app then handles the routing internally via the DeepLinkHandler.
 */
config.devServer = config.devServer || {};
config.devServer.historyApiFallback = {
    // Exclude API calls and static assets from the fallback
    rewrites: [
        {
            from: /^\/(?!api|\.)/,
            to: '/index.html'
        }
    ]
};
