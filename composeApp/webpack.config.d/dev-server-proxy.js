// Proxy /api/ requests from the WASM webpack dev server (port 8082) to the
// backend Ktor server (port 8080). This allows the Kotlin/Wasm app to use
// relative URLs like "/api/savefiles" during local development without
// running into cross-origin issues.
//
// This file is picked up automatically by the Kotlin/Wasm webpack build.
// It only affects the development server (wasmJsBrowserDevelopmentRun),
// not the production bundle.
config.devServer = config.devServer || {};
// Use port 8082 so it doesn't conflict with the Ktor backend on 8080.
config.devServer.port = config.devServer.port || 8082;
config.devServer.proxy = [
    {
        context: ["/api"],
        target: "http://localhost:8080",
        changeOrigin: true
    }
];
