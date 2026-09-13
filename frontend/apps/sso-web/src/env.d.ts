/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SSO_API: string;
  readonly VITE_APP_TITLE: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
