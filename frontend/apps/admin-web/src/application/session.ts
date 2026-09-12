import { createBrowserSessionStore } from '@namewta/adapter-storage-browser';

export const session = createBrowserSessionStore({ key: 'Admin-Token' });

export const getToken = () => session.getToken();
export const setToken = (access_token: string) => session.setToken(access_token);
export const removeToken = () => session.clear();
