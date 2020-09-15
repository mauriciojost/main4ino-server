# Setup

```
# Install postgres
sudo apt-get install postgresql-all

# Ensure service is up and running
sudo service postgres restart

# Connect to the database
sudo -u postgres -i
psql

# Change the password of the user postgres
ALTER USER postgres WITH PASSWORD 'password';

# Create the database
CREATE DATABASE main4inoprd;

```

A typical configuration is:

```
driver = "org.postgresql.Driver"
url = "jdbc:postgresql://localhost:5432/main4inoprd"
user = "postgres"
password = "password"
```

To be able to connect to any DB from any user do:

```
The problem is still your pg_hba.conf file (/etc/postgresql/*/main/pg_hba.conf*).

This line:

local   all             postgres                                peer
Should be:
local   all             postgres                                md5
```

Then you can run: 

```
psql dbname postgres
```

Shortcuts:
```
\q           to exit
\? command   for help on command
```

