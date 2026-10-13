# ---------- Etapa 1: build ----------
# Imagem com Maven + JDK 21 só para compilar; não vai para a imagem final.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia só o pom.xml primeiro para o Docker cachear as dependências baixadas
# e não precisar rebaixar tudo a cada mudança de código-fonte.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# ---------- Etapa 2: runtime ----------
# Imagem final só com o JRE (não o JDK completo) e o .jar já pronto,
# bem menor do que a imagem de build.
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
