// const { ipcRenderer } = require("electron");

// ipcRenderer.on("open-img", (event, filePath) => {
//     // document.getElementById("gmm").setAttribute("src", filePath);
// })

pageReady(function () {
    new Vue({
        el: '#app',
        data () {
            return {
                useConfig:{
                    hitsNum: 1
                },
                indexConfig: {
                    path: null,
                    spliter: null,
                    readLineNum: 1
                },
                showProgress: false,
                progressText: "0% ",
                progressNum : 0
            }
        },
        computed: {
            progressStyle () {
                return {
                    "width": `${this.progressNum}%`
                }
            }
        },
        methods: {
            listenProgress(serial) {
                axios.get('http://localhost:7777/fileIndex/index/' + serial).then((response) => {
                    if (response.status === 200) {
                        let resData = response.data;
                        if (resData.status === "000000") {
                            const progress = resData.data;
                            this.progressNum = progress.progress || this.progressNum;
                            this.progressText = progress.descr;
                            // console.info(progress)
                            if (1 === progress.status) {
                                // 进行中，继续监听
                                this.listenProgress(serial);
                            } else if (2 === progress.status) {
                                // 进度完成
                                this.$message.success("索引完成");
                            } else {
                                // 失败
                                this.$message.error("索引失败！~");
                            }
                        }
                    }
                });
            },
            startIndex() {
                this.saveIndexConfig(() => {
                    this.progressNum = 0;
                    this.showProgress = true;

                    axios.put('http://localhost:7777/fileIndex/index').then((response) => {
                        if (response.status === 200) {
                            let resData = response.data;
                            if (resData.status === "000000") {
                                const serial = resData.data;
                                // 监听进度
                                this.listenProgress(serial);
                            }
                        }
                    });
                })
            },
            saveUseConfig() {
                this.saveConfig({...this.useConfig});
            },
            saveIndexConfig(callback) {
                this.saveConfig({...this.indexConfig}, callback);
            },
            saveConfig(config, callback) {
                axios.put('http://localhost:7777/config', config).then((response) => {
                    if (response.status === 200) {
                        let resData = response.data;
                        if (resData.status === "000000") {
                            this.$message.success("保存配置成功！");
                            callback && callback instanceof Function && callback();
                        }
                    }
                });
            },
            loadConfig() {
                axios.get('http://localhost:7777/config').then((response) => {
                    if (response.status === 200) {
                        let resData = response.data;
                        if (resData.status === "000000") {
                            const configMap = resData.data || {}
                            this.useConfig.hitsNum = configMap.hitsNum
                            this.indexConfig.path = configMap.path
                            this.indexConfig.spliter = configMap.spliter
                            this.indexConfig.readLineNum = configMap.readLlineNum
                        }
                    }
                });
                // 页面跳转测试
                // window.docNative.navigatePage("pages/settings.html")
            }
        },
        created () {
            this.loadConfig();
        }
    })
})