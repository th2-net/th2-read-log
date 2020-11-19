# Log Reader User Manual 1.00

Document Information
====================

Table of contents
=================

{table_contents}

Introduction
============
Log reader read text log files, line by line and applying regex expression to each line. Results are sending to RabbitMQ.
Log reader produces **raw messages**. See **RawMessage** type in infra.proto.

Quick start
=============

Configuration
=============

**GRPC_HOST** - specifying GRCP host

GRPC_HOST=0.0.0.0 (default value)

**GRPC_PORT** - specifying GRPC port 

GRPC_PORT=80808 (default value)

**GRPC_CONNECTIVITY_HOST** - specifying GRPC host to connectivity service

GRPC_CONNECTIVITY_HOST=0.0.0.0 (default value)

**GRPC_CONNECTIVITY_PORT** - specifying GRPC port to connectivity service

GRPC_CONNECTIVITY_PORTR=8181 (default value)

**RABBITMQ_EXCHANGE_NAME_TH2_CONNECTIVITY** - specifing exchange name

RABBITMQ_EXCHANGE_NAME_TH2_CONNECTIVITY=demo_exchange (default value)

**RABBITMQ_HOST** - rabbitmq host

RABBITMQ_HOST=10.0.0.12 

**RABBITMQ_PORT** - rabbitmq port

RABBITMQ_PORT=5672

**RABBITMQ_VHOST** - rabbitmq vhost

RABBITMQ_VHOST=vhost

**RABBITMQ_USER** - rabbitmq user

RABBITMQ_USER=user

**RABBITMQ_PASS** - rabbitmq user password

RABBITMQ_PASS=password

**RABBITMQ_QUEUE_POSTFIX** - specifying postfix for generated queue names

RABBITMQ_QUEUE_POSTFIX = prod.  This will produces two queues: logreader.first.prod and datareader.second.prod

If not specified  -  will produces two queues: logreader.first.default and datareader.second.default

**LOG_FILE_NAME** - specifying path where log file is located

**REGEX** - regular expression to parse string

**REGEX_GROUP** - specifying regex group to be sending

If not specified - will send all matched groups. 

**BATCH_PER_SECOND_LIMIT** - the limit for batch publications per second. NOTE: counting only batches. The number of messages inside the batch does not affect the publication rate.
If not specified the publication will be unlimited (useful for small files).

Examples
=============

**Example 1**

Parse all FIX messages

Input string: 6 2015-02-09 16:23:59,455 DEBUG   (FixService(NFT2)    ) - 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=mit123\u00011137=9\u000110=027\u0001
Regex: 8=FIX.+10=.+?
Regex group: 0 
Output: 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=mit123\u00011137=9\u000110=0

**Example 2**

Parse FIX messages only from specified lines

Input string: 6 2015-02-09 16:23:59,455 DEBUG   (FixService(NFT2)    ) - 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=mit123\u00011137=9\u000110=027\u0001
Regex: (FixService.+)(8=FIX.+10=.+?)
Regex group: 2 
Output: 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=mit123\u00011137=9\u000110=0

