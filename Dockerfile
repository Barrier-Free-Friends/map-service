FROM bellsoft/liberica-openjdk-alpine:21
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", \
            "-DDB_URL=${ENV_DB_URL}", \
            "-DDB_USERNAME=${ENV_DB_USERNAME}", \
            "-DDB_PASSWORD=${ENV_DB_PASSWORD}", \
            "-DDB_DDL_AUTO=${ENV_DB_DDL_AUTO}", \
            "app.jar"]

EXPOSE 3334