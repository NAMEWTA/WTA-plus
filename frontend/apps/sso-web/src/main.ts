import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import '@namewta/web-kit-ui-element/theme.css';
import { initializeTheme } from '@namewta/web-kit-ui-element/theme';
import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import AuthorizePage from './views/AuthorizePage.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', component: AuthorizePage },
    { path: '/authorize', component: AuthorizePage },
    { path: '/login', component: AuthorizePage }
  ]
});

initializeTheme();

createApp(App).use(router).mount('#app');
