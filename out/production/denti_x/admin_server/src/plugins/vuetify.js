import Vue from 'vue';
import Vuetify from 'vuetify/lib/framework';
import '@/styles/sass/import/_index.scss'; // _index.scss를 한번만 불러오면 자동으로 css로 컴파일 해줌
import { en, ko } from 'vuetify/es5/locale';
Vue.use(Vuetify);

export default new Vuetify({
  lang: {
    locales: { ko, en },
    current: 'ko',
  },
  theme: {
    dark: false,
    default: 'light',
    disable: false,
    options: {
      cspNonce: undefined,
      customProperties: undefined,
      minifyTheme: undefined,
      themeCache: undefined,
    },
    themes: {
      light: {
        primary: '#0544e8',
        primaryDark: '#020541',
        btnBlack: '#35363B',
        darkGrey: '#2c2d3c',
        secondary: '#424242',
        accent: '#82B1FF',
        error: '#EF4242',
        info: '#2196F3',
        success: '#1CC966',
        warning: '#FB8C00',
        inputBG: '#fcfcfc',
        selectedBlue: '#1251fc',
        white: '#fff',
      },
      dark: {
        primary: '#2196F3',
        secondary: '#424242',
        accent: '#FF4081',
        error: '#FF5252',
        info: '#2196F3',
        success: '#4CAF50',
        warning: '#FB8C00',
      },
    },
  },
});
