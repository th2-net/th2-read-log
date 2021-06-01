# Log Reader User Manual 3.0.0

## Document Information

### Introduction

Log reader read text log files, line by line and applying regex expression to each line. Results are sending to RabbitMQ.
Log reader produces **raw messages**. See **RawMessage** type in infra.proto.

### Quick start
General view of the component will look like this:
```yaml
apiVersion: th2.exactpro.com/v1
kind: Th2Box
metadata:
  name: read-log
spec:
  image-name: ghcr.io/th2-net/th2-read-log
  image-version: <image version>
  type: th2-read
  custom-config:
      logDirectory: "log/dir"
      aliases:
        A:
          regexp: ".*",
          pathFilter: "fileA.*\\.log"
        B:
          regexp: ".*",
          pathFilter: "fileB.*\\.log"
          groups: [ 0, 1 ]
        C:
          regexp: ".*",
          pathFilter: "fileC.*\\.log"
          timestampRegexp: "^202.+?(?= QUICK)"
          timestampFormat: "yyyy-MM-dd HH:mm:ss"
      common:
        staleTimeout: "PT1S"
        maxBatchSize: 100
        maxPublicationDelay: "PT5S"
        leaveLastFileOpen: false
        fixTimestamp: false
        maxBatchesPerSecond: -1 # unlimited
      pullingInterval: "PT5S"
  pins:
    - name: read_log_out
      connection-type: mq
      attributes: ['raw', 'publish', 'store']
  extended-settings:
    service:
      enabled: false
    envVariables:
      JAVA_TOOL_OPTIONS: "-XX:+ExitOnOutOfMemoryError"
    mounting:
      - path: "<destination path in Kubernetes pod>"
        pvcName: <Kubernetes persistent volume component name >
    resources:
      # Min system requirments ...
      limits:
        memory: 200Mi
        cpu: 200m
      requests:
        memory: 100Mi
        cpu: 50m
```

#### Configuration

##### Reader configuration

+ logDirectory - the directory to watch files
+ aliases - the mapping between alias and files that correspond to that alias
    + pathFilter - filter for files that correspond to that alias
    + regexp - the regular expression to extract data from the source lines
    + groups - the groups' indexes to extract from line after matching the regexp. If not specified all groups will be published
    + timestampRegexp - the regular expression to extract the timestamp from the log's line.
      If _timestampRegexp_ is **not** specified the message's timestamp will be generated automatically (no additional data is added to the message).
      If it is set then:
        + If the _timestampFormat_ specified the timestamp will be used as a message timestamp.
        + Otherwise, the timestamp will be added to the message properties, but the message timestamp will be generated.
    + timestampFormat - the format for the timestamp extract from the log's line. **Works only with specified _timestampRegexp_ parameter**.
      If _timestampFormat_ is specified the timestamp extract with _timestampRegexp_ will be parsed using this format and used as a message's timestamp.
+ common - the common configuration for read core. Please found the description [here](https://github.com/th2-net/th2-read-file-common-core/blob/master/README.md#configuration).
  NOTE: the fields with `Duration` type should be described in the following format `PT<number><time unit>`.
  Supported time units (**H** - hours,**M** - minutes,**S** - seconds). E.g. PT5S - 5 seconds, PT5M - 5 minutes, PT0.001S - 1 millisecond
+ pullingInterval - how often the directory will be checked for updates after not updates is received

##### Pin declaration

The log reader requires a single pin with _publish_ and _raw_ attributes. The data is published in a raw format. To use it please conect the output pin with another pin that transforms raw data to parsed data. E.g. the **codec** box.

Example:
```yaml
apiVersion: th2.exactpro.com/v1
kind: Th2Box
metadata:
  name: log-reader
spec:
  pins:
    - name: out_log
      connection-type: mq
      attributes: ['raw', 'publish', 'store']
```

### Examples

#### Example 1

Parse all FIX messages

Input string: 6 2015-02-09 16:23:59,455 DEBUG   (FixService(NFT2)    ) - 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=123\u00011137=9\u000110=027\u0001
Regex: 8=FIX.+10=.+?
Regex group: 0 
Output: 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=123\u00011137=9\u000110=0

#### Example 2

Parse FIX messages only from specified lines

Input string: 6 2015-02-09 16:23:59,455 DEBUG   (FixService(NFT2)    ) - 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=123\u00011137=9\u000110=027\u0001
Regex: (FixService.+)(8=FIX.+10=.+?)
Regex group: 2 
Output: 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=123\u00011137=9\u000110=0

## Changes

### 3.0.0

+ Migrate to a common read-core