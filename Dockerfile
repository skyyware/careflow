FROM node:26-bookworm-slim AS frontend
WORKDIR /workspace

COPY frontend/package*.json ./frontend/
RUN npm --prefix frontend ci

COPY frontend ./frontend
RUN npm --prefix frontend run build

FROM maven:3.9.11-eclipse-temurin-21 AS backend
WORKDIR /workspace

COPY backend/pom.xml ./backend/pom.xml
RUN mvn -f backend/pom.xml -B dependency:go-offline

COPY backend ./backend
RUN rm -rf backend/src/main/resources/META-INF/resources/* \
  && mkdir -p backend/src/main/resources/META-INF/resources
COPY --from=frontend /workspace/frontend/dist/frontend/browser/ ./backend/src/main/resources/META-INF/resources/
RUN mvn -f backend/pom.xml -B test package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && useradd --system --create-home --home-dir /home/careflow careflow
COPY --from=backend /workspace/backend/target/quarkus-app/ ./

ENV PORT=8080
EXPOSE 8080
USER careflow

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
