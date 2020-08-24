FROM maven:3.6.0-jdk-11-slim AS build
COPY src /app/src
COPY pom.xml /app
RUN mvn -f /app/pom.xml clean package


FROM openjdk:11-jre-slim
ENV RABBITMQ_HOST=host \
    RABBITMQ_PORT=port \
    RABBITMQ_VHOST=vhost \
    RABBITMQ_USER=user \
    RABBITMQ_PASS=password \
    RABBITMQ_EXCHANGE_NAME_TH2_CONNECTIVITY=demo_exchange \
    LOG_FILE_NAME=filename \
	REGEX=regex \
	REGEX_GROUP=regex_group 
	
COPY --from=build /app/target/logreader-SNAPSHOT-0.0.1-jar-with-dependencies.jar /usr/local/lib/logreader.jar

ENTRYPOINT ["java","-jar","/usr/local/lib/logreader.jar"]
