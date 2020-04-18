# README

For `haproxy` use the following settings:

```
  frontend fe
          bind *:8080
          default_backend be

  backend be
          balance roundrobin
          server app1 127.0.0.1:8090 check
          server app2 127.0.0.1:8091 check
          server app3 127.0.0.1:8092 check
```
