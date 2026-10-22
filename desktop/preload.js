const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  resolveFullStream: (data) => ipcRenderer.invoke('resolve-full-stream', data),
  getVideoEmbedUrl: (data) => ipcRenderer.invoke('get-video-embed-url', data),
  resolveDirectVideo: (videoId) => ipcRenderer.invoke('resolve-direct-video', videoId),
  searchYouTubeMusic: (query, filterType) => ipcRenderer.invoke('search-yt-music', query, filterType),
  browseYouTube: (browseId) => ipcRenderer.invoke('browse-yt', browseId),
  resolveArtistProfile: (name) => ipcRenderer.invoke('resolve-artist-profile', name)
});
