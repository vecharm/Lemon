# Lemon
## 重构Android-Pomelo，将Java-websocket 替换为 okhttp-websocket<br>

#### Pomelo 使用的是Java-websocket，在某些手机会出现内存泄露，所以本项目使用Okhttp-websocket代替<br>

##### * 去掉xhr-polling 过时协议的支持<br>
##### * 重写重连机制的逻辑，Pomelo里面的重连机制，重连一次就挂掉了<br>
##### * 去掉多Connection，多namespace 的支持，很多时候都用不上。为了从简，就去掉多余的东西了<br>
##### * 支持SSL，Okhttp的功能<br>
![两个版本连接耗时对比测试](https://github.com/VipMinF/Lemon/tree/master/screenshoot/device-2018-04-20-150540.png)

## 使用方式：
 ```Java
     //如果不支持ssl，请注释这一条
        Connector.setDefaultSSL(SSLFactory.genSSLSocket(), SSLFactory.trustManager)
        
        client = LemonClient(host, port, object : SocketListener {
            override fun onReConnect() {
                Toaster.showInfo("socket.onReConnect")
            }

            override fun onConnected() {
                Toaster.showInfo("socket.onConnected")
            }

            override fun onError(socketIOException: SocketIOException) {
                Toaster.showInfo("socket.onError")
            }

            override fun onDisconnect() {
                Toaster.showInfo("socket.onDisconnect")
            }
           //如果不需要内部的重连机制  reconnectTime = -1
        },reconnectTime = 3000)
```

