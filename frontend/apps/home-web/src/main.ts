import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import '@namewta/web-kit-ui-element/theme.css';
import '@namewta/web-kit-ui-element/shell.css';
import { initializeIcons } from '@namewta/web-kit-ui-element/icons';
import { initializeTheme } from '@namewta/web-kit-ui-element/theme';
import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';

initializeTheme();
initializeIcons();

createApp(App).use(createPinia()).use(router).use(ElementPlus).mount('#app');
