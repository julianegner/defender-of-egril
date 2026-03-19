import { lazy, Suspense } from "react";
import type { KcContext } from "keycloakify/login/KcContext";
import DefaultPage from "keycloakify/login/DefaultPage";
import UserProfileFormFields from "keycloakify/login/UserProfileFormFields";
import { useI18n } from "./i18n";
import Template from "./Template";

// Lazy-load the Login page to keep the initial bundle small.
const Login = lazy(() => import("keycloakify/login/pages/Login"));

/**
 * Class overrides shared by every page.
 *
 * With doUseDefaultCss=false, keycloakify's kcClsx() emits the literal KC
 * key names as HTML class names (e.g. "kcFormPasswordVisibilityIconShow")
 * instead of the PatternFly aliases ("fa fa-eye").  The `classes` prop adds
 * extra class names so Font Awesome renders the eye / eye-slash icons.
 */
const sharedClasses = {
  kcFormPasswordVisibilityIconShow: "fa fa-eye",
  kcFormPasswordVisibilityIconHide: "fa fa-eye-slash",
} as const;

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
                classes={sharedClasses}
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
                classes={sharedClasses}
              />
            );
        }
      })()}
    </Suspense>
  );
}
