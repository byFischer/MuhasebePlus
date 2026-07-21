const { app, BrowserWindow, shell } = require('electron');
const { spawn } = require('child_process');
let autoUpdater = null;
try { autoUpdater = require('electron-updater').autoUpdater; } catch {}
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const log = require('electron-log');

log.transports.file.level = 'info';

let backendProcess = null;
let mainWindow = null;
let splashWindow = null;

// Packaged vs dev modunda kaynak yolunu döndürür
function getResourcePath(rel) {
  return app.isPackaged
    ? path.join(process.resourcesPath, rel)
    : path.join(__dirname, '..', rel);
}

// JWT secret'ı userData'da saklar — uygulama ömrü boyunca sabit kalır
function getOrCreateJwtSecret() {
  const file = path.join(app.getPath('userData'), 'jwt.secret');
  if (!fs.existsSync(file)) {
    const secret = crypto.randomBytes(64).toString('hex');
    fs.writeFileSync(file, secret, { encoding: 'utf8' });
  }
  return fs.readFileSync(file, 'utf8').trim();
}

function spawnBackend() {
  const javaBin = app.isPackaged
    ? path.join(process.resourcesPath, 'jre', 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
    : 'java';

  const jarPath = getResourcePath('backend.jar');
  const userDataDir = app.getPath('userData');

  log.info(`Starting backend: ${javaBin} -jar ${jarPath}`);

  backendProcess = spawn(javaBin, [
    '-jar', jarPath,
    '--spring.profiles.active=desktop',
    `--app.data.dir=${path.join(userDataDir, 'data')}`,
  ], {
    env: {
      ...process.env,
      JWT_SECRET: getOrCreateJwtSecret(),
      HOME: userDataDir,
      USERPROFILE: userDataDir,
      LC_ALL: 'en_US.UTF-8',
      LANG: 'en_US.UTF-8',
    },
    cwd: userDataDir,
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  backendProcess.stdout.on('data', d => log.info(`[backend] ${d.toString().trim()}`));
  backendProcess.stderr.on('data', d => log.warn(`[backend] ${d.toString().trim()}`));
  backendProcess.on('exit', (code) => log.info(`Backend exited with code ${code}`));
}

async function waitForBackend(timeoutMs = 90000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    try {
      const res = await fetch('http://localhost:8080/actuator/health');
      if (res.ok) {
        log.info('Backend is ready');
        return;
      }
    } catch {
      // henüz hazır değil, bekliyoruz
    }
    await new Promise(r => setTimeout(r, 600));
  }
  throw new Error('Backend failed to start within timeout');
}

function createSplash() {
  splashWindow = new BrowserWindow({
    width: 420,
    height: 280,
    frame: false,
    alwaysOnTop: true,
    resizable: false,
    transparent: false,
    webPreferences: { nodeIntegration: false, contextIsolation: true },
  });
  splashWindow.loadFile(path.join(__dirname, 'splash.html'));
}

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 900,
    minHeight: 600,
    show: false,
    title: 'MuhasebePlus',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  mainWindow.loadFile(path.join(__dirname, 'frontend', 'index.html'));

  mainWindow.once('ready-to-show', () => {
    if (splashWindow) {
      splashWindow.close();
      splashWindow = null;
    }
    mainWindow.show();
    mainWindow.focus();
  });

  // Harici linkleri tarayıcıda aç
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });
}

app.whenReady().then(async () => {
  createSplash();

  try {
    spawnBackend();
    await waitForBackend();
    createMainWindow();

    if (app.isPackaged && autoUpdater) {
      autoUpdater.logger = log;
      autoUpdater.autoDownload = true;
      autoUpdater.checkForUpdatesAndNotify();
    }
  } catch (err) {
    log.error('Startup error:', err);
    const { dialog } = require('electron');
    dialog.showErrorBox(
      'Başlatma Hatası',
      `Uygulama başlatılamadı:\n${err.message}\n\nLütfen uygulamayı yeniden başlatın.`
    );
    app.quit();
  }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('will-quit', () => {
  if (backendProcess) {
    log.info('Stopping backend...');
    backendProcess.kill('SIGTERM');
    backendProcess = null;
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) createMainWindow();
});

if (autoUpdater) {
  autoUpdater.on('update-available', () => {
    log.info('Update available, downloading...');
  });

  autoUpdater.on('update-downloaded', () => {
    log.info('Update downloaded — will install on next restart');
    const { dialog } = require('electron');
    dialog.showMessageBox({
      type: 'info',
      title: 'Güncelleme Hazır',
      message: 'Yeni bir güncelleme indirildi. Şimdi yeniden başlatmak ister misiniz?',
      buttons: ['Şimdi Yeniden Başlat', 'Daha Sonra'],
    }).then(({ response }) => {
      if (response === 0) autoUpdater.quitAndInstall();
    });
  });
}
