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

  const { msg, msgStr } = i18n;

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

  // Set the browser tab title dynamically from the Keycloak realm name so that
  // it reads "Log in to {realm}" (or the localised equivalent) instead of the
  // static "Egril Keycloak Theme" value from index.html.
  useEffect(() => {
    const realm = "realm" in kcContext
      ? (kcContext.realm as Record<string, unknown>)
      : undefined;
    const displayName = (realm?.displayName ?? realm?.name) as string | undefined;
    if (displayName) {
      document.title = msgStr("loginTitle", displayName);
    }
  }, [msgStr]);

  // Belt-and-suspenders: inline CSS that forces the password-input-group
  // grid layout and button appearance. Uses the actual KC class names that
  // doUseDefaultCss=false generates (kcInputGroup, kcFormPasswordVisibilityButtonClass)
  // rather than the PatternFly pf-c-* aliases.
  const btnBg = darkMode ? "#555" : "#d0d0d0";
  const btnBorder = darkMode ? "#666" : "#bbb";
  const btnColor = darkMode ? "#eee" : "#333";
  const passwordGroupCss = `
    .kcInputGroup{display:grid!important;grid-template-columns:1fr 44px!important;align-items:stretch!important;width:100%!important;margin-bottom:4px!important}
    .kcInputGroup input[type="password"]{width:100%!important;border-right:none!important;border-radius:4px 0 0 4px!important;margin-bottom:0!important}
    .kcFormPasswordVisibilityButtonClass{width:44px!important;min-height:42px!important;padding:0!important;border:1px solid ${btnBorder}!important;border-radius:0 4px 4px 0!important;cursor:pointer!important;font-size:1.1rem!important;display:flex!important;align-items:center!important;justify-content:center!important;background-color:${btnBg}!important;color:${btnColor}!important}
    .kcFormPasswordVisibilityButtonClass:hover{filter:brightness(1.15)!important}
  `;

  return (
    <>
    {/* Inject password-group styles after all other stylesheets so they win */}
    <style dangerouslySetInnerHTML={{ __html: passwordGroupCss }} />
    <div className={`egril-root ${darkMode ? "dark" : "light"}`}>
      {/* ── Brand banner row: Defender of Egril (left) and cosha.nu (right) ── */}
      <div className="brand-banner-row">
        <img
          src={`${import.meta.env.BASE_URL}images/defender_of_egril.png`}
          alt="Defender of Egril"
          className="defender-banner-img"
        />
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

            {/* Registration section – only shown on the login page when the realm allows it */}
            {kcContext.pageId === "login.ftl" && (() => {
              const loginCtx = kcContext as {
                realm?: { registrationAllowed?: boolean };
                url?: { registrationUrl?: string };
              };
              return loginCtx.realm?.registrationAllowed && loginCtx.url?.registrationUrl
                ? (
                  <div className="register-section">
                    <p className="register-label">{msg("noAccount")}</p>
                    <a href={loginCtx.url.registrationUrl} className="register-btn">
                      {msg("doRegister")}
                    </a>
                  </div>
                )
                : null;
            })()}

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
    </>
  );
}
