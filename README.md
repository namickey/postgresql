# postgresql 11

# install for centos 8
```
postgresql11-libs-11.17-1PGDG.rhel8.x86_64.rpm
postgresql11-11.17-1PGDG.rhel8.x86_64.rpm
postgresql11-server-11.17-1PGDG.rhel8.x86_64.rpm
```

```
sudo /usr/pgsql-11/bin/postgresql-11-setup initdb
sudo systemctl enable postgresql-11
sudo systemctl start postgresql-11
```

https://qiita.com/tom-sato/items/d5f722fd02ed76db5440

# password_encryption
sudo vi /var/lib/pgsql/11/data/postgresql.conf
```
#password_encryption = md5		# md5 or scram-sha-256
 ↓
password_encryption = scram-sha-256		# md5 or scram-sha-256
```
```
sudo systemctl restart postgresql-11
```

# connect
```
sudo su - postgres
psql

show password_encryption;
```

# change password
```
ALTER USER postgres with encrypted password 'postgres';
quit
```
```
exit
```

# change auth
sudo vi /var/lib/pgsql/11/data/pg_hba.conf
```
local   all             all                                     peer
 ↓
local   all             postgres                                scram-sha-256
```
```
sudo systemctl restart postgresql-11
```

# connect
```
psql -U postgres
```

# CREATE USER
```
-- ユーザー名「user1」をパスワード「pass」で作成する
CREATE USER user1 WITH PASSWORD 'pass';
quit

psql -U user1 -d postgres
```

# TLS

https://dk521123.hatenablog.com/entry/2020/05/05/221239

```
sudo su - postgres

cd /var/lib/pgsql/11/data
openssl genrsa 2048 > server.key
openssl req -new -key server.key > server.csr
openssl x509 -req -signkey server.key < server.csr > server.crt

chmod 600 server.key
chmod 600 server.csr
chmod 600 server.crt
```

sudo vi /var/lib/pgsql/11/data/pg_hba.conf
```
local   all             postgres                                scram-sha-256
 ↓
hostssl   all           postgres           0.0.0.0/0            scram-sha-256
```

sudo vi /var/lib/pgsql/11/data/postgresql.conf
```
#ssl=off
 ↓
ssl=on

#listen_addresses = 'localhost'
 ↓
listen_addresses = '*'
```
```
psql -h 192.168.1.12 -U user1 -d postgres
```
