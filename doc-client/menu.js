const { Menu, BrowserWindow, ipcMain }  = require("electron");

// 菜单设置
const menu = Menu.buildFromTemplate([{
    label: "选项",
    submenu: [
        {
            label: "搜索",
            click: function () {
                // let win = BrowserWindow.getFocusedWindow()
                // win.loadFile("pages/index.html")
                ipcMain.emit("navigatePage", "pages/index.html");
            }
        },
        {
            label: "配置",
            click: function () {
                ipcMain.emit("navigatePage", "pages/settings.html");
            }
        },
        {
            label: "开发者工具",
            click: function () {
                // 开启调试工具
                win = BrowserWindow.getFocusedWindow()
                win.webContents.openDevTools();
            }
        }
    ]
}]);

Menu.setApplicationMenu(menu)
// Menu.setApplicationMenu(null)
// win = BrowserWindow.getFocusedWindow()
// win.setMenu(menu)