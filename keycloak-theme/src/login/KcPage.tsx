import { lazy, Suspense } from "react";
import type { KcContext } from "keycloakify/login/KcContext";
import DefaultPage from "keycloakify/login/DefaultPage";
import UserProfileFormFields from "keycloakify/login/UserProfileFormFields";
import { useI18n } from "./i18n";
import Template from "./Template";

// Lazy-load the Login page to keep the initial bundle small.
const Login = lazy(() => import("keycloakify/login/pages/Login"));

export default function KcPage({ kcContext }: { kcContext: KcContext }) {
  const { i18n } = useI18n({ kcContext });

  return (
    <Suspense>
      {(() => {
        switch (kcContext.pageId) {
          case "login.ftl":
            return (
              <Login
                Template={Template}
                kcContext={kcContext}
                i18n={i18n}
                doUseDefaultCss={false}
              />
            );
          default:
            return (
              <DefaultPage
                kcContext={kcContext}
                i18n={i18n}
                doUseDefaultCss={false}
                Template={Template}
                UserProfileFormFields={UserProfileFormFields}
                doMakeUserConfirmPassword={false}
              />
            );
        }
      })()}
    </Suspense>
  );
}
