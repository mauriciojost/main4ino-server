# README

For `haproxy` use the settings of the provided `haproxy.cfg` file.

You can do a simple symlink.

```
rm /etc/haproxy/haproxy.cfg
ln -sf $HOME/main4ino-server/misc/haproxy/haproxy.cfg /etc/haproxy/haproxy.cfg
```

For websocket issues, take a look at: 

https://medium.com/@lucjansuski/investigating-websocket-haproxy-reconnecting-and-timeouts-6d19cc0002a1
