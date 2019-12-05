#启动
1. docker-compose up -d 启动容器，mysql已经打开了binlog。mysql用户名admin密码password。执行sql检查binlog时候开启:
```sql
show variables like '%log_bin%'
```

2. 如果要打印DML的sql，需要修改my.cnf
```
binlog-format=Mixed
```

3. 下载canal-deployer，并解压
https://github.com/alibaba/canal/releases/tag/canal-1.1.4
4. 修改配置conf/example/instance.properties

```properties
canal.instance.dbUsername=root
canal.instance.dbPassword=password
```

5. 启动canal

```shell
bin/startup.sh
```

6. 运行工程中的Main.java，操作mysql中的表和数据库，更新删除数据都会在console中看到对应输出