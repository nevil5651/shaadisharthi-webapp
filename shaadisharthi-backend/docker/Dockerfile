# ============================
# Stage 1: Build WAR with Maven
# ============================
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy Maven configuration and source
COPY shaadisharthi-backend/pom.xml .
COPY shaadisharthi-backend/src ./src

# Build WAR file (skip tests for speed)
RUN mvn clean package -DskipTests


# ============================
# Stage 2: Run with Tomcat
# ============================
FROM tomcat:9.0-jdk17

# Remove default ROOT app
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy WAR built from previous stage
COPY --from=builder /app/target/*.war /tmp/app.war

# Extract WAR into ROOT
RUN mkdir -p /usr/local/tomcat/webapps/ROOT \
 && cd /usr/local/tomcat/webapps/ROOT \
 && jar -xvf /tmp/app.war

# Copy entrypoint and schema (corrected paths)
COPY shaadisharthi-backend/docker/entrypoint.sh /entrypoint.sh
COPY shaadisharthi-backend/docker/schema.sql /docker-entrypoint-initdb.d/schema.sql

RUN chmod +x /entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]

