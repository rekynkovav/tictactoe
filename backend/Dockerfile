FROM eclipse-temurin:17-jre-alpine

# Создадим директорию приложения
WORKDIR /app

# Копируем JAR файл с точным именем
COPY target/tictactoe-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]