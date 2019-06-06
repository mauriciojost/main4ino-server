# README

This section helps you create the configuration files for your server. 

1. Generate a salt: 

```
  sbt "test:console" 
  println(com.github.t3hnar.bcrypt.generateSalt)"
```

2. Generate credentials using any random string generator (20 chars).

3. Put it in input.conf .

4. Modify add.conf according to the accounts to be created (for devices and actual users).
5. Launch

```
./generate
```

6. You will have a security.conf file that you can use for your server. You are done.
