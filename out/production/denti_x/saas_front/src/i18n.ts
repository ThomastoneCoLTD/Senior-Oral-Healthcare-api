import i18n from "i18next";
import { initReactI18next } from "react-i18next";

import ko from "./locales/ko.json";
import en from "./locales/en.json";
import vi from "./locales/vi.json";

i18n.use(initReactI18next).init({
  resources: {
    ko: { translation: ko },
    en: { translation: en },
    vi: { translation: vi },
  },
  lng: localStorage.getItem("lang") || "ko",
  fallbackLng: "en",
  interpolation: { escapeValue: false },
});

export default i18n;
