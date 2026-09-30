import HighLight from '@highlightjs/vue-plugin';
import { initializeIcons } from '@namewta/web-kit-ui-element/icons';
import { initializeTheme } from '@namewta/web-kit-ui-element/theme';
import { ElDialog } from 'element-plus';
import { createApp } from 'vue';
import 'virtual:uno.css';
import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import '@namewta/web-kit-ui-element/theme.css';
import '@namewta/web-kit-ui-element/shell.css';
import VxeUIPlugin, { VxeUI } from 'vxe-pc-ui';
import VxeTablePlugin from 'vxe-table';
import '@/assets/styles/index.scss';
import 'highlight.js/styles/atom-one-dark.css';
import 'highlight.js/lib/common';
import 'virtual:svg-icons-register';
import 'vxe-pc-ui/lib/style.css';
import 'vxe-table/lib/style.css';
import ElementIcons from '@/application/host/element-icons';
import i18n from '@/lang/index';
import App from './App.vue';
import directive from './directive';
import './permission';
import router from './router';
import store from './store';

VxeUI.setConfig({
  zIndex: 999999
});

ElDialog.props.closeOnClickModal.default = false;

initializeTheme();
initializeIcons();

const app = createApp(App);

app.use(HighLight);
app.use(ElementIcons);
app.use(store);
app.use(router);
app.use(i18n);
app.use(VxeUIPlugin);
app.use(VxeTablePlugin);
directive(app);

app.mount('#app');
