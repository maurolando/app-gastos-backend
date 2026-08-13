# Build en dos etapas: la imagen final no lleva Maven ni el codigo fuente.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Las dependencias se resuelven en una capa aparte: mientras el pom no cambie,
# Docker la reutiliza y el build no vuelve a bajar medio Maven Central.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Los tests corren en CI contra H2; repetirlos aca solo alarga cada deploy.
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario sin privilegios: por defecto la imagen corre como root.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /app/target/*.jar app.jar

# El plan gratuito de Render da 512 MB. Sin tope explicito la JVM se cree dueña
# de toda la memoria del contenedor y el OOM killer la mata bajo carga.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"

# Render inyecta PORT y application.properties lo lee con server.port.
EXPOSE 8080

# Sin wrapper de shell: java queda como PID 1 y recibe el SIGTERM del apagado.
ENTRYPOINT ["java", "-jar", "app.jar"]
