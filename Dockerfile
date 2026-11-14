FROM maven:3.9-eclipse-temurin-17 AS compilacion
WORKDIR /construccion
COPY pom.xml ./
COPY src/main ./src/main
RUN mvn -B -ntp -Dmaven.test.skip=true package org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies -DincludeScope=runtime

FROM eclipse-temurin:17-jre-jammy
ENV TZ=America/Bogota APLOMO_PUERTO=8080 APLOMO_CARPETA_FOTOS=/app/fotos
WORKDIR /app
RUN groupadd --gid 10001 aplomo && useradd --uid 10001 --gid aplomo --no-create-home --shell /usr/sbin/nologin aplomo \
    && mkdir -p /app/fotos /app/demo && chown -R aplomo:aplomo /app/fotos
COPY --from=compilacion /construccion/target/aplomo-1.0-SNAPSHOT.jar /app/aplomo.jar
COPY --from=compilacion /construccion/target/dependency/ /app/lib/
COPY src/main/resources/static/img/muestra_perfil.jpg /app/demo/muestra_perfil.jpg
USER aplomo
EXPOSE 8080
ENTRYPOINT ["java","-Duser.timezone=America/Bogota","-cp","/app/aplomo.jar:/app/lib/*","co.aplomo.App"]
