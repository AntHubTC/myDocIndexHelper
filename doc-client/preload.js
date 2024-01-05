const { contextBridge, ipcRenderer } = require('electron/renderer')

contextBridge.exposeInMainWorld('docNative', {
    navigatePage: (pageName) => {
        ipcRenderer.send('navigatePage', pageName)
    }
})