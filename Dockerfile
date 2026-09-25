FROM node:20-alpine AS web
WORKDIR /web
COPY farm-react/package*.json ./
RUN npm ci
COPY farm-react/ ./
RUN npx vite build --outDir /static

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY farm-web/pom.xml .
COPY farm-web/src ./src
COPY --from=web /static ./src/main/resources/static
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/farm-web-1.0.0.jar app.jar
ENV JAVA_OPTS="-Xmx350m"
EXPOSE 8090
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
