const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  resolveFullStream: (data) => ipcRenderer.invoke('resolve-full-stream', data),
  searchYouTubeMusic: (query) => ipcRenderer.invoke('search-yt-music', query)
});
