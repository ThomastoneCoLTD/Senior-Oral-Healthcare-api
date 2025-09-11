module.exports = {
  root: true,
  env: {
    node: true,
  },
  extends: [
    'plugin:vue/base', // eslint plugin vue 설정 (base < essential < strongly-recommended < recommended)
    'eslint:recommended', // eslint rules 강도
    'plugin:prettier/recommended', // prettier
    '@vue/prettier',
  ],
  parserOptions: {
    parser: 'babel-eslint',
  },
  rules: {
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'prettier/prettier': [
      'warn',
      {
        singleQuote: true, // 따옴표는 홑따옴표를 사용한다. (jsx는 이 설정을 무시한다.)
        semi: true, // 모든 구문에 세미콜론을 붙인다.
        useTabs: false, // Tab을 사용 안 한다.
        tabWidth: 2, // 들여쓰기 할 때, 기본 폭을 설정한다.
        trailingComma: 'all', // 객체, 배열 등에서 항상 마지막에 , 를 붙인다.
        printWidth: 200, // 한 줄에서 wrap 이 되는 기준의 글자 수를 정한다.
        bracketSpacing: true, // 객체 리터럴의 괄호 사이에 공백을 출력
        arrowParens: 'avoid', // 화살표 함수에서 단일 파라미터에 괄호를 붙일지("always") 말지("avoid")에 대한 여부를 결정한다.
        jsxBracketSameLine: true, // JSX의 닫는 괄호를 줄바꿈을 하지않을 것인지 여부
        bracketSameLine: true, // HTML Tag의 위치 여부
        htmlWhitespaceSensitivity: 'ignore', // HTML tag 사이 공백 처리 여부
      },
    ],
  },
};
