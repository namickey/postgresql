# postgresql 11

# platform

- CentOS Stream 8


# install for centos 8

https://lets.postgresql.jp/

https://www.postgresql.org/download/
```
# Install the repository RPM:
sudo dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm

# Disable the built-in PostgreSQL module:
sudo dnf -qy module disable postgresql

# Install PostgreSQL:
sudo dnf install -y postgresql11-server

# Optionally initialize the database and enable automatic start:
sudo /usr/pgsql-11/bin/postgresql-11-setup initdb
sudo systemctl enable postgresql-11
sudo systemctl start postgresql-11
```
```
インストールされたrpm
postgresql11-libs-11.17-1PGDG.rhel8.x86_64.rpm
postgresql11-11.17-1PGDG.rhel8.x86_64.rpm
postgresql11-server-11.17-1PGDG.rhel8.x86_64.rpm
```


# change pgdata

https://ex1.m-yabe.com/archives/4719

https://access.redhat.com/documentation/ja-jp/red_hat_enterprise_linux/7/html/selinux_users_and_administrators_guide/sect-managing_confined_services-postgresql-configuration_examples

create pgdata dir
```
sudo mkdir -p /postgre/pgdata
sudo chmod 700 /postgre/pgdata
sudo chown postgres:postgres /postgre/pgdata
```
```
sudo systemctl stop postgresql-11
```

vi /usr/lib/systemd/system/postgresql-11.service
```
Environment=PGDATA=/var/lib/pgsql/11/data/
 ↓
Environment=PGDATA=/postgre/pgdata/
```

```
sudo systemctl daemon-reload
sudo postgresql-11-setup initdb
sudo systemctl start postgresql-11
sudo systemctl status postgresql-11
```

# wal archive log

https://logical-studio.com/develop/backend/20201008-for-the-day-when-db-is-corrupted/#WAL

https://www.kimullaa.com/posts/201910271500/


# password_encryption

https://qiita.com/tom-sato/items/d5f722fd02ed76db5440

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

# firewalld

https://nwengblog.com/centos-firewalld/

```
sudo firewall-cmd --zone=public --add-port=5432/tcp --permanent
sudo firewall-cmd --reload
```

# tcp dump

https://qiita.com/tossh/items/4cd33693965ef231bd2a
```
sudo tcpdump -A dst port 5432
```

```
TLS通信でselect文発行　※select文が見えない
13:27:04.109616 IP 192.168.1.19.58376 > asrockcentos8.postgres: Flags [P.], seq 3504:3573, ack 4186, win 509, length 69
E..m..@................8=.....w.P...........@..p....&.<.C.jIi].....A..o..G..5...0....d..+X.....PZ.>&......._.
13:27:04.111666 IP 192.168.1.19.58376 > asrockcentos8.postgres: Flags [P.], seq 3573:3653, ack 4224, win 509, length 80
E..x..@................8=.....w.P...........KR...U..UY....xW....c........       .iW|9 ...r..<..W.1..),.O[.cQ...^...u..AM.`..+.
13:27:04.162607 IP 192.168.1.19.58376 > asrockcentos8.postgres: Flags [.], ack 4296, win 509, length 0
E..(..@....P...........8=..^..x)P.............
```

```
平文通信でselect文発行　※select文が見える
13:36:20.823661 IP 192.168.1.19.58532 > asrockcentos8.postgres: Flags [P.], seq 1135:1185, ack 1273, win 508, length 50
E..Z.V@................8....0!..P....0..P....S_1....B.....S_1.......D....P.E... .....S....
13:36:20.825161 IP 192.168.1.19.58532 > asrockcentos8.postgres: Flags [P.], seq 1185:1282, ack 1299, win 508, length 97
E....W@................8....0!..P...0...P.../S_2.select * from item where id = $1.......B.....S_2...........12345..D....P.E...  .....S....
13:36:20.868347 IP 192.168.1.19.58532 > asrockcentos8.postgres: Flags [.], ack 1405, win 507, length 0
E..(.Z@................8...60!..P....-........
```

# summary

spring-boot2.6.6 + postgresql-42.3.3 ⇒ postgresql-11

application.properties (spring-boot)
```
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://192.168.1.12:5432/postgres
spring.datasource.username=postgres
spring.datasource.password=postgres
```

postgresql設定とTLS通信
```
ssl = off  +  local   + TCPDUMP =  (tcpdumpが取得できない...)
ssl = off  +  host    + TCPDUMP =  平文通信
ssl = on   +  host    + TCPDUMP =  TLS通信
ssl = on   +  hostssl + TCPDUMP =  TLS通信
```

# backup

https://www.postgresql.jp/document/11/html/app-pgbasebackup.html

https://gihyo.jp/dev/feature/01/dex_postgresql/0004

https://tecsak.hatenablog.com/entry/2021/01/02/224329

https://www.fujitsu.com/jp/products/software/resources/feature-stories/postgres/backup-recovery/



