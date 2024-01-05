const { app, BrowserWindow, ipcMain } = require("electron");
const { spawn } = require('child_process');
const path = require('node:path')

function javaApplication(succCallback, closeCallback, errCallback) {
    // 启动 Java 服务端
    const javaProcess = spawn('java', ['-jar', '../doc-server/target/doc-server-1.1.0.jar']);

    // 监听 Java 进程的输出
    javaProcess.stdout.on('data', (data) => {
        const log = data.toString()
        console.log(`Java stdout: ${log}`);
        if (log && log.indexOf("started on port") != -1) {
            console.log("服务端已经启动，启动主窗口")
            succCallback && succCallback();
        }
        if (log && (log.indexOf("Web server failed to start") != -1 || log.indexOf("Java stderr: Error:") != -1)) {
            // 启动失败直接退出
            app.quit();
        }
    });

    javaProcess.stderr.on('data', (data) => {
        console.error(`Java stderr: ${data}`);
        errCallback && errCallback()
    });

    javaProcess.on('close', (code) => {
        console.log(`Java process exited with code ${code}`);
        closeCallback && closeCallback()
    });
}

app.whenReady().then(() => {
    // 开始加载应用
    ipcMain.emit("loading")
})
// app.on('ready',createWindow);
app.on('window-all-closed',() => {
    app.quit();
}); 
// app.on('activate',() => {
//     if(win == null){
//         createWindow();
//     }
// })


// 加载应用
ipcMain.on("loading", () => {
    console.info("loading start")
    let loadingWin = new BrowserWindow({
        width:400,
        height:200,
        frame:false, // 是否有边框
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false,
            enableRemoteModule: true
        }
    });
    loadingWin.loadFile('loading.html');
    // 开启调试工具
    // win.webContents.openDevTools();
    loadingWin.on('close',() => {
        //回收BrowserWindow对象
        loadingWin = null;
    });
    // w1.on('resize',() => {
    //     win.reload();
    // })

    javaApplication(() => {
        console.info("java server started")
        // 这里如何通过事件通知electron主进程，主进程接受事件然后打开新的窗口
        ipcMain.emit("loaded", loadingWin)
    });
});


// 启动主应用

ipcMain.on("loaded", loadingWin => {
    console.info("start main GUI")

    let mainWin = new BrowserWindow({
        title: "Document index Viewer",
        width:800,
        height:600,
        frame: true, // 是否有边框
        show: false,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false,
            enableRemoteModule: true
        },
        webPreferences: {
            preload: path.join(__dirname, 'preload.js')
        }
    });
    require("./menu.js")

    // w1.on('resize',() => {
    //     win.reload();
    // })
    mainWin.on('close',() => {
        //回收BrowserWindow对象
        mainWin = null;
    });
    // win.loadURL(`file://${__dirname}/index.html`);
    mainWin.loadFile('pages/index.html').then(() => {
        // 关闭加载对话框
        loadingWin.close();
        // 显示主窗口
        mainWin.show();
        console.info("main.html loaded")
    });
    ipcMain.on('navigatePage', (page, pageName) => {
        page = pageName || page;
        mainWin.loadFile(page).then(() => {
            console.info(page + " loaded")
        });
    })
});