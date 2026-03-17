import { type ReactNode, useEffect } from "react";
import type { TemplateProps } from "keycloakify/login/TemplateProps";
import { useInitialize } from "keycloakify/login/Template.useInitialize";
import type { KcContext } from "keycloakify/login/KcContext";
import type { I18n } from "./i18n";
import "./template.css";

/**
 * Reads the KEYCLOAKIFY_DARK_MODE cookie that is set by the Defender of
 * Egril web app before redirecting to Keycloak for login.
 * Falls back to the system color-scheme preference.
 */
function isDarkModeEnabled(): boolean {
  const cookie = document.cookie
    .split("; ")
    .find(row => row.startsWith("KEYCLOAKIFY_DARK_MODE="));
  if (cookie !== undefined) {
    return cookie.split("=")[1] === "dark";
  }
  // Fallback: honor the operating-system / browser preference.
  return window.matchMedia("(prefers-color-scheme: dark)").matches;
}

/**
 * Returns true if the Keycloak message should be rendered on the page.
 * Warnings produced by app-initiated actions are suppressed because they
 * are shown by the application itself after the redirect.
 */
function shouldDisplayMessage(ctx: KcContext): boolean {
  if (!("message" in ctx) || ctx.message === undefined) {
    return false;
  }
  // Show warnings only when not in an app-initiated action flow.
  if (ctx.message.type === "warning" && "isAppInitiatedAction" in ctx && ctx.isAppInitiatedAction) {
    return false;
  }
  return true;
}

/**
 * Returns the client baseUrl as a string if it is available, or undefined.
 */
function getClientBaseUrl(ctx: KcContext): string | undefined {
  if (!("client" in ctx) || ctx.client === undefined) {
    return undefined;
  }
  const baseUrl = (ctx.client as Record<string, unknown>)["baseUrl"];
  return typeof baseUrl === "string" && baseUrl.length > 0 ? baseUrl : undefined;
}

export default function Template(props: TemplateProps<KcContext, I18n>) {
  const {
    kcContext,
    i18n,
    doUseDefaultCss: _doUseDefaultCss,
    classes: _classes,
    children,
    headerNode,
    socialProvidersNode
  } = props;

  const { msg } = i18n;

  useInitialize({ kcContext, doUseDefaultCss: false });

  const darkMode = isDarkModeEnabled();

  // Apply dark/light class to the body so the full-page background matches the theme.
  // This covers the .login-pf body selector used by PatternFly / Keycloak's base CSS.
  useEffect(() => {
    document.body.classList.toggle("dark", darkMode);
    document.body.classList.toggle("light", !darkMode);
    return () => {
      document.body.classList.remove("dark", "light");
    };
  }, [darkMode]);

  return (
    <div className={`egril-root ${darkMode ? "dark" : "light"}`}>
      {/* ── Top banner: Defender of Egril ── */}
      <div className="brand-banner defender-banner">
        <img
          src={`${import.meta.env.BASE_URL}images/defender_of_egril.png`}
          alt="Defender of Egril"
          className="defender-banner-img"
        />
      </div>

      {/* ── Second banner: cosha.nu ── */}
      <div className="brand-banner coshanu-banner">
        <img
          src={`${import.meta.env.BASE_URL}images/coshanu.png`}
          alt="cosha.nu"
          className="coshanu-banner-img"
        />
      </div>

      {/* ── Main login card ── */}
      <main className="login-main">
        <div className="login-card">
          {/* Realm/app header */}
          <div className="login-card-header">
            {headerNode !== null && (
              <h1 className="login-card-title">{headerNode}</h1>
            )}
          </div>

          {/* Page content (form, error messages, etc.) */}
          <div className="login-card-body">
            {/* Info / error messages from Keycloak */}
            {shouldDisplayMessage(kcContext) && "message" in kcContext && kcContext.message && (
              <div
                className={`kc-alert kc-alert-${kcContext.message.type}`}
                role="alert"
              >
                <span
                  dangerouslySetInnerHTML={{
                    __html: kcContext.message.summary
                  }}
                />
              </div>
            )}

            {children}

            {socialProvidersNode !== null && (
              <div className="social-providers">{socialProvidersNode}</div>
            )}
          </div>

          {/* Back-to-app link */}
          {(() => {
            const baseUrl = getClientBaseUrl(kcContext);
            return baseUrl !== undefined ? (
              <div className="login-card-footer">
                <a href={baseUrl}>{msg("backToApplication")}</a>
              </div>
            ) : null;
          })()}
        </div>
      </main>
    </div>
  );
}
