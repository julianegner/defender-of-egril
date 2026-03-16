import { type ReactNode } from "react";
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

  return (
    <div className={`egril-root ${darkMode ? "dark" : "light"}`}>
      {/* ── Top banner: Defender of Egril ── */}
      <div className="brand-banner defender-banner">
        {/* Left: game characters (SVG) */}
        <svg
          className="game-icons"
          viewBox="0 0 180 80"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          {/* Goblin – small green creature */}
          <ellipse cx="30" cy="48" rx="12" ry="14" fill="#4caf50" />
          <circle cx="30" cy="28" r="9" fill="#388e3c" />
          <circle cx="26" cy="26" r="2" fill="#1b5e20" />
          <circle cx="34" cy="26" r="2" fill="#1b5e20" />
          <ellipse cx="28" cy="20" rx="3" ry="5" fill="#4caf50" transform="rotate(-20 28 20)" />
          <ellipse cx="33" cy="19" rx="3" ry="5" fill="#4caf50" transform="rotate(15 33 19)" />
          <rect x="24" y="52" width="4" height="12" rx="2" fill="#4caf50" />
          <rect x="32" y="52" width="4" height="12" rx="2" fill="#4caf50" />

          {/* Ork – broader purple creature with horns */}
          <ellipse cx="90" cy="48" rx="15" ry="16" fill="#7b1fa2" />
          <circle cx="90" cy="26" r="11" fill="#6a1b9a" />
          <circle cx="85" cy="24" r="2.5" fill="#4a148c" />
          <circle cx="95" cy="24" r="2.5" fill="#4a148c" />
          <line x1="83" y1="16" x2="80" y2="8" stroke="#6a1b9a" strokeWidth="3" strokeLinecap="round" />
          <line x1="97" y1="16" x2="100" y2="8" stroke="#6a1b9a" strokeWidth="3" strokeLinecap="round" />
          <rect x="83" y="52" width="5" height="14" rx="2.5" fill="#7b1fa2" />
          <rect x="92" y="52" width="5" height="14" rx="2.5" fill="#7b1fa2" />

          {/* Wizard tower – trapezoid with star */}
          <polygon points="138,68 162,68 158,38 142,38" fill="#ffa000" stroke="#ff6f00" strokeWidth="1.5" />
          <rect x="140" y="34" width="6" height="8" rx="1" fill="#ffa000" stroke="#ff6f00" strokeWidth="1" />
          <rect x="148" y="34" width="6" height="8" rx="1" fill="#ffa000" stroke="#ff6f00" strokeWidth="1" />
          <rect x="156" y="34" width="6" height="8" rx="1" fill="#ffa000" stroke="#ff6f00" strokeWidth="1" />
          {/* Star */}
          <polygon
            points="150,44 152,50 158,50 153,54 155,60 150,56 145,60 147,54 142,50 148,50"
            fill="#ffeb3b"
          />
        </svg>

        {/* Centre: "Defender of Egril" text */}
        <div className="defender-title">
          <span className="defender-of">Defender of</span>
          <span className="egril-text">Egril</span>
        </div>

        {/* Right: shield image */}
        <img
          src={`${import.meta.env.BASE_URL}images/black-shield.png`}
          alt="Defender of Egril shield"
          className="shield-img"
        />
      </div>

      {/* ── Second banner: cosha.nu ── */}
      <div className="brand-banner coshanu-banner">
        {/* Coloured geometric shapes */}
        <div className="coshanu-shapes" aria-hidden="true">
          {/* Hexagon (green) */}
          <svg viewBox="0 0 40 40" width="36" height="36">
            <polygon points="20,2 35,11 35,29 20,38 5,29 5,11" fill="#00e676" />
          </svg>
          {/* Octagon (light gray) */}
          <svg viewBox="0 0 40 40" width="34" height="34">
            <polygon
              points="12,2 28,2 38,12 38,28 28,38 12,38 2,28 2,12"
              fill="#bdbdbd"
            />
          </svg>
          {/* Pentagon (magenta) */}
          <svg viewBox="0 0 40 40" width="34" height="34">
            <polygon points="20,2 37,14 31,35 9,35 3,14" fill="#e040fb" />
          </svg>
          {/* Triangle (blue) */}
          <svg viewBox="0 0 40 40" width="32" height="32">
            <polygon points="20,4 38,36 2,36" fill="#448aff" />
          </svg>
          {/* Square (cyan) */}
          <svg viewBox="0 0 40 40" width="28" height="28">
            <rect x="4" y="4" width="32" height="32" fill="#00e5ff" />
          </svg>
          {/* Circle (red) */}
          <svg viewBox="0 0 40 40" width="32" height="32">
            <circle cx="20" cy="20" r="18" fill="#ff1744" />
          </svg>
        </div>
        <span className="coshanu-text">cosha.nu</span>
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
