# SETUP

In the root contab `crontab -e`:
```
* * * * * reniced -o cmd
```

In the `/etc/reniced.conf`:

```
-2 postgres
-2 main4ino
-2 org.mauritania.main4ino.Server
```

