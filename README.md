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

# connect
```
sudo su - postgres
psql
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

# change password
```
ALTER USER postgres with encrypted password 'postgres';
```

sudo vi /var/lib/pgsql/11/data/pg_hba.conf
```
local   all             all                                     peer
 ↓
local   all             postgres                                md5
```
```
sudo systemctl restart postgresql-11
```
