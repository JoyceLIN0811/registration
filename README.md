# registration Project

這是一個不重複使用者 username 註冊申請機制,若遇到重複的 username,則在末尾自
動後綴阿拉伯數字。

### Data Base 建置需求
``` shell script
db.Users.createIndex( { "username": 1 }, { unique: true } )
```

### Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./gradlew quarkusDev
```

### 前端頁面
``` shell script
註冊頁： http://localhost:8080/registration.html
後台頁： http://localhost:8080/back-stage.html
```

## Packaging and running the application

The application can be packaged using:
```shell script
./gradlew build
```
It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./gradlew build -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

### docker 打包部署
``` shell script
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/registration-jvm .
docker run -i --rm -p 8080:8080 quarkus/registration-jvm
```

