FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
RUN apk add --no-cache curl
# Copia el jar generado
COPY target/*.jar app.jar

# Puerto expuesto
EXPOSE 8082

# Variables de entorno configurables
ENV SPRING_PROFILES_ACTIVE=default
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]