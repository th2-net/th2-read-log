FROM openjdk:12-alpine

ENV RABBITMQ_HOST=host \
    RABBITMQ_PORT=port \
    RABBITMQ_VHOST=vhost \
    RABBITMQ_USER=user \
    RABBITMQ_PASS=password \
    RABBITMQ_EXCHANGE_NAME_TH2_CONNECTIVITY=demo_exchange \
    CSV_FILE_NAME=filename

WORKDIR /home
COPY ./ .
ENTRYPOINT ["/home/th2-csv-reader/bin/th2-csv-reader"]
