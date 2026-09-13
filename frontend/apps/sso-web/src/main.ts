import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';
import AuthorizePage from './views/AuthorizePage.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: AuthorizePage },
    { path: '/authorize', component: AuthorizePage },
    { path: '/login', component: AuthorizePage }
  ]
});

createApp(App).use(router).mount('#app');
