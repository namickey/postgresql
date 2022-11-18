# postgresql 11

# platform

- CentOS Stream 8


# install postgresql for centos Stream 8

https://lets.postgresql.jp/

https://www.postgresql.org/download/
```
# Install the repository RPM:
sudo dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm

# Disable the built-in PostgreSQL module:
sudo dnf -qy module disable postgresql

# Install PostgreSQL:
sudo dnf install -y postgresql11-server
```
```
インストールされたrpm
postgresql11-libs-11.17-1PGDG.rhel8.x86_64.rpm
postgresql11-11.17-1PGDG.rhel8.x86_64.rpm
postgresql11-server-11.17-1PGDG.rhel8.x86_64.rpm
```

# install postgresql for Amazon Linux 2

https://qiita.com/libra_lt/items/f2d2d8ee389daf21d3fb

# ディレクト構成

```
/postgre
├── pgdata             # PGDATAディレクトリを'/var/lib/pgsql/11/data/'から切り替える
│   ├── base
│   ├── global
│   ├── log
│   ├── pg_commit_ts
│   ├── pg_dynshmem
│   ├── pg_logical
│   ├── pg_multixact
│   ├── pg_notify
│   ├── pg_replslot
│   ├── pg_serial
│   ├── pg_snapshots
│   ├── pg_stat
│   ├── pg_stat_tmp
│   ├── pg_subtrans
│   ├── pg_tblspc
│   ├── pg_twophase
│   ├── pg_wal
│   └── pg_xact
├── pgdata.bak        # リストア作業時にPGDATAディレクトリの一時退避先
│   ├── base
│   ├── global
│   ├── log
│   ├── pg_commit_ts
│   ├── pg_dynshmem
│   ├── pg_logical
│   ├── pg_multixact
│   ├── pg_notify
│   ├── pg_replslot
│   ├── pg_serial
│   ├── pg_snapshots
│   ├── pg_stat
│   ├── pg_stat_tmp
│   ├── pg_subtrans
│   ├── pg_tblspc
│   ├── pg_twophase
│   ├── pg_wal
│   └── pg_xact
├── basebackup       # フルバックアップのtarファイル格納先
└── wal_archive      # WALログアーカイブ先
```

# change pgdata

PGDATAディレクトリを`/var/lib/pgsql/11/data/`から`/postgre/pgdata`へ切り替える

https://ex1.m-yabe.com/archives/4719

https://access.redhat.com/documentation/ja-jp/red_hat_enterprise_linux/7/html/selinux_users_and_administrators_guide/sect-managing_confined_services-postgresql-configuration_examples

create pgdata dir
```
sudo mkdir -p /postgre/pgdata
sudo chmod 700 /postgre/pgdata
sudo chown postgres:postgres /postgre
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

# change postgres home directory

https://www.softel.co.jp/blogs/tech/archives/5931

```
echo ~postgres
sudo usermod -d /postgre/pgdata postgres
echo ~postgres
```

# start service
```
sudo systemctl daemon-reload
sudo postgresql-11-setup initdb
sudo systemctl restart postgresql-11
sudo systemctl status postgresql-11

sudo su - postgres
psql
SHOW data_directory;
quit
```

# change password_encryption

パスワードの暗号化方法を切り替える

https://qiita.com/tom-sato/items/d5f722fd02ed76db5440

sudo vi /postgre/pgdata/postgresql.conf
```
#password_encryption = md5		# md5 or scram-sha-256
 ↓
password_encryption = scram-sha-256		# md5 or scram-sha-256
```
```
sudo systemctl restart postgresql-11
```

```
sudo su - postgres
psql

show password_encryption;
quit
```

# set postgres password

postgresユーザへパスワードを設定する

```
sudo su - postgres
psql

ALTER USER postgres with encrypted password 'postgres';
quit
```

# change auth
sudo vi /postgre/pgdata/pg_hba.conf
```
local   all             all                                peer
 ↓
local   all             all                                scram-sha-256
```
```
sudo systemctl restart postgresql-11
```
```
sudo su - postgres
psql -U postgres
quit
```

# CREATE USER
```
sudo su - postgres
psql -U postgres

-- ユーザー名「user1」をパスワード「pass」で作成する
CREATE USER user1 WITH PASSWORD 'pass';
quit

psql -U user1 -d postgres
quit
```

# TLS

図解 X.509 証明書  
https://qiita.com/TakahikoKawasaki/items/4c35ac38c52978805c69  

https://dk521123.hatenablog.com/entry/2020/05/05/221239

パターン1
```
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 > private_key.pem
openssl pkey -text -noout -in private_key.pem

openssl req -x509 -days 3650 -key private_key.pem -subj /CN=example.com > certificate.pem
openssl x509 -text -noout -in certificate.pem

chmod 600 private_key.pem
chmod 600 certificate.pem

mv private_key.pem server.key
mv certificate.pem server.crt
```

パターン2
```
sudo su - postgres

cd /postgre/pgdata
openssl genrsa 2048 > server.key
openssl req -new -key server.key > server.csr
openssl x509 -req -signkey server.key < server.csr > server.crt

chmod 600 server.key
chmod 600 server.csr  # 不要
chmod 600 server.crt
```

sudo vi /postgre/pgdata/pg_hba.conf
```
local   all             postgres                                scram-sha-256
 ↓
hostssl   all           postgres           localhost            scram-sha-256
 or
hostssl   all           postgres           0.0.0.0/0            scram-sha-256
```

sudo vi /postgre/pgdata/postgresql.conf
```
#ssl=off
 ↓
ssl=on

#listen_addresses = 'localhost'
 ↓
listen_addresses = '*'
```
```
sudo su - postgres
psql -h 192.168.1.12 -U user1 -d postgres
quit
```

# firewalld

https://nwengblog.com/centos-firewalld/

```
sudo firewall-cmd --list-all
sudo firewall-cmd --zone=public --add-port=5432/tcp --permanent
sudo firewall-cmd --reload
sudo firewall-cmd --list-all
```

# tcp dump

https://qiita.com/tossh/items/4cd33693965ef231bd2a
```
複数サーバ間通信
sudo tcpdump -A dst port 5432

単一サーバ内通信
sudo tcpdump -i lo -A dst port 5432
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

# TLS summary

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
1. ssl=off  +  host    + password      + TCPDUMP =  password：平文通信、SQL：平文通信
2. ssl=off  +  host    + scram-sha-256 + TCPDUMP =  password：判読不可、SQL：平文通信
3. ssl=on   +  hostssl + password      + TCPDUMP =  password：判読不可、SQL：判読不可
4. ssl=on   +  hostssl + scram-sha-256 + TCPDUMP =  password：判読不可、SQL：判読不可
```

# wal archive log

https://www.postgresql.jp/document/11/html/continuous-archiving.html#BACKUP-PITR-RECOVERY

https://logical-studio.com/develop/backend/20201008-for-the-day-when-db-is-corrupted/#WAL

https://www.kimullaa.com/posts/201910271500/

sudo vi /postgre/pgdata/postgresql.conf
```
wal_level = replica
archive_mode = on
archive_command = 'test ! -f /postgre/wal_archive/%f && gzip < %p > /postgre/wal_archive/%f'
#archive_command = 'test ! -f /postgre/wal_archive/%f && cp %p /postgre/wal_archive/%f'
```

```
sudo su - postgres
mkdir /postgre/wal_archive
chmod 700 /postgre/wal_archive
```


# backup

https://www.postgresql.jp/document/11/html/app-pgbasebackup.html

https://gihyo.jp/dev/feature/01/dex_postgresql/0004

https://tecsak.hatenablog.com/entry/2021/01/02/224329

https://www.fujitsu.com/jp/products/software/resources/feature-stories/postgres/backup-recovery/

https://www.lancard.com/blog/2018/03/22/pg_basebackup%E3%82%92%E8%A9%A6%E3%81%99/

初回バックアップ
```
sudo su - postgres
mkdir /postgre/basebackup
chmod 700 /postgre/basebackup

pg_basebackup -D /postgre/basebackup/ -Ft -z -Xs -P -U postgres
#pg_basebackup -D /postgre/basebackup/ -Fp -Xs -P -U postgres
```

二回目以降バックアップはディレクトリを空にしてから実行する
```
sudo rm /postgre/basebackup/base.tar.gz
sudo rm /postgre/basebackup/pg_wal.tar.gz

pg_basebackup -D /postgre/basebackup/ -Ft -z -Xs -P -U postgres
#pg_basebackup -D /postgre/basebackup/ -Fp -Xs -P -U postgres
```

# restore

１．停止
```
sudo systemctl stop postgresql-11
```

２．PGDATA（最新WALログ）の退避
```
sudo mv /postgre/pgdata/ /postgre/pgdata.bak/
sudo mkdir /postgre/pgdata
sudo chmod 700 /postgre/pgdata
sudo chown postgres:postgres /postgre/pgdata
```

３．ベースバックアップの復旧
```
tar xzfv /postgre/basebackup/base.tar.gz -C /postgre/pgdata
```

４．古いWALログの削除
```
rm -rf /postgre/pgdata/pg_wal
```

５．最新WALログの復旧
```
cp -p /postgre/pgdata.bak/pg_wal/ /postgre/pgdata/pg_wal/
```

６．リカバリ設定
vi /postgre/pgdata/recovery.conf
```
restore_command = 'gunzip < /postgre/wal_archive/%f > %p'
#restore_command = 'cp /postgre/wal_archive/%f "%p"'
```

７．起動
```
sudo systemctl start postgresql-11
```
