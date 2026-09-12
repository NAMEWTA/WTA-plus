import { createBrowserSessionStore } from '@namewta/adapter-storage-browser';

export const session = createBrowserSessionStore({ key: 'Home-Token' });
export const getToken = () => session.getToken();
export const removeToken = () => session.clear();
