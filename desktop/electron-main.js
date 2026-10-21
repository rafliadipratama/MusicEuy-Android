const { app, BrowserWindow } = require('electron');
const path = require('path');

// Disable incompatible Vulkan on Linux Wayland to guarantee silky smooth rendering
app.commandLine.appendSwitch('disable-features', 'Vulkan');

function createWindow() {
  const win = new BrowserWindow({
    width: 1280,
    height: 840,
    minWidth: 960,
    minHeight: 640,
    backgroundColor: '#0B0D10',
    title: 'SoraFy',
    icon: path.join(__dirname, 'src/assets/logo.png'),
    autoHideMenuBar: true,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: false // allow cross-origin streams & covers
    }
  });

  win.loadFile(path.join(__dirname, 'src/index.html'));
  win.setMenuBarVisibility(false);
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
