FROM gradle:6.6-jdk11 AS build
ARG release_version
COPY ./ .
RUN gradle clean build dockerPrepare -Prelease_version=${release_version}

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
WORKDIR /home
COPY --from=build /home/gradle/build/docker .
ENTRYPOINT ["/home/service/bin/service"]
