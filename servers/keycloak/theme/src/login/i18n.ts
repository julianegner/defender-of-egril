import { i18nBuilder } from "keycloakify/login";

const { useI18n, ofTypeI18n } = i18nBuilder
  .withCustomTranslations({
    en: {
      registrationEmailLoginHint: "You can log in with your email address even if your username is not your email address."
    },
    de: {
      registrationEmailLoginHint: "Sie können sich mit Ihrer E-Mail-Adresse anmelden, auch wenn Ihr Benutzername keine E-Mail-Adresse ist."
    },
    es: {
      registrationEmailLoginHint: "Puede iniciar sesión con su dirección de correo electrónico aunque su nombre de usuario no sea su dirección de correo."
    },
    fr: {
      registrationEmailLoginHint: "Vous pouvez vous connecter avec votre adresse e-mail même si votre nom d'utilisateur n'est pas votre adresse e-mail."
    },
    it: {
      registrationEmailLoginHint: "Puoi accedere con il tuo indirizzo email anche se il tuo nome utente non è il tuo indirizzo email."
    }
  })
  .build();

export { useI18n, ofTypeI18n };
export type I18n = typeof ofTypeI18n;
