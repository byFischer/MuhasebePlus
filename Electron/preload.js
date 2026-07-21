const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electron', {
  getVersion: () => ipcRenderer.invoke('get-version'),
  openLogs: () => ipcRenderer.invoke('open-logs'),
  platform: process.platform,
});
