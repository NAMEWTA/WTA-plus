import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import Harness from './Harness.vue';

createApp(Harness).use(ElementPlus).mount('#app');
