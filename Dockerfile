# ===== Build stage =====
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copia wrapper e pom para cachear dependências
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copia o código e empacota
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/uploads && chown -R spring:spring /app
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
