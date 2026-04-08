import { StrictMode, lazy, Suspense } from "react";
import { createRoot } from "react-dom/client";
import type { KcContext } from "keycloakify/login/KcContext";

const KcPage = lazy(() => import("./login/KcPage"));

// kcContext is injected by Keycloak when the theme is active.
// During local development it will be undefined.
const kcContext = (window as unknown as { kcContext: KcContext | undefined }).kcContext;

if (kcContext === undefined) {
  throw new Error(
    [
      "This app is a Keycloakify theme and is not meant to run standalone.",
      "See: https://docs.keycloakify.dev/testing-your-theme"
    ].join("\n")
  );
}

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <Suspense>
      <KcPage kcContext={kcContext} />
    </Suspense>
  </StrictMode>
);
