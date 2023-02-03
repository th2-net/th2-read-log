# Log Reader User Manual 4.0.0

## Document Information

### Introduction

Log reader read text log files, line by line and applying regex expression to each line. Results are sending to RabbitMQ.
Log reader produces **raw messages**. See **RawMessage** type in [common.proto](https://github.com/th2-net/th2-grpc-common/tree/master/src/main/proto/th2_grpc_common).

### Important information

This read can be used to read the file that system writes down in real time. To achieve that the following requirements should be matched:

+ the system must only append data to the file
+ if you synchronise the file from the remote machine to the one where the read is deployed the new data should be appended to **the same file**

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
      syncWithCradle: true
      aliases:
        A:
          regexp: ".*"
          pathFilter: "fileA.*\\.log"
          directionRegexps:
            FIRST: "incoming"
            SECOND: "outgoing"
        B:
          regexp: "(.*)(\\d+)(.*)"
          pathFilter: "fileB.*\\.log"
          groups: [ 0, 1 ]
        C:
          regexp: ".*"
          pathFilter: "fileC.*\\.log"
          timestampRegexp: "^202.+?(?= QUICK)"
          timestampFormat: "yyyy-MM-dd HH:mm:ss"
          timestampZone: UTC
          skipBefore: "2022-10-31T12:00:00Z"
        D:
          regexp: ".*"
          pathFilter: "fileC.*\\.log"
          joinGroups: true
          groupsJoinDelimiter: "\t"
          headersFormat:
            HeaderA: "${0}"
            HeaderB: "const ${groupName}"
            HeaderC: "just const"
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
      # Min system requirements ...
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
+ syncWithCradle - enables synchronization with Cradle for timestamps and sequences that correspond to the alias
+ aliases - the mapping between alias and files that correspond to that alias
    + pathFilter - filter for files that correspond to that alias
    + regexp - the regular expression to extract data from the source lines
    + directionRegexps - the map from direction to regexp to determine the direction for source line.
      If the line does not match this pattern it will be skipped for this direction.
      By default, all lines correspond to the FIRST direction.
    + groups - the groups' indexes to extract from line after matching the regexp. If not specified all groups will be published.
      **NOTE: reader ignores this parameter if _joinGroups_ is used.**
    + timestampRegexp - the regular expression to extract the timestamp from the log's line.
      If _timestampRegexp_ is **not** specified the message's timestamp will be generated automatically (no additional data is added to the message).
      If it is set then:
        + The extracted timestamp will be added to the message's properties.
        + If the _timestampFormat_ specified the timestamp will be used as a message timestamp. Otherwise, the message's timestamp will be generated.
    + timestampFormat - the format for the timestamp extract from the log's line. **Works only with specified _timestampRegexp_ parameter**.
      If _timestampFormat_ is specified the timestamp extract with _timestampRegexp_ will be parsed using this format and used as a message's timestamp.
    + timestampZone - the zone which should be used to process the timestamp from the log file
    + skipBefore - the parameter defines the minimum timestamp in UTC (ISO format) for log messages.
      If log message has timestamp less than the specified one it will be dropped.
      **NOTE: the parameter only works if 'timestampRegexp' and 'timestampFormat' are specified**
    + joinGroups - enables joining groups into a message in CSV format. Can be used to extract generic data from the log. Disabled by default.
    + groupsJoinDelimiter - the delimiter that will be used to join groups from the _regexp_ parameter. **Works only if _joinGroups_ is enabled**. The default value is `,`.
    + headersFormat - the headers' definition. The reader uses the keys as headers. The value to the key will be converted to a value for each match in the current line.
      You can use the following syntax to refer to the group in the regexp:
      + ${index} - reference by group index. E.g. `${1}`. Please note, that the group `0` is the whole regexp
      + ${groupName} - reference by group name. E.g. `${groupA}`
      + Constant values - the other data that does not match the format above will be taken as is.
        <br/>
        Example: you have the following line: `line for test 42 groups`. And the following regexp: `(\\S+)\\s(?<named_group>\\d+)`. It matches this: `test 42`.
        If you specify headers like this:
        ```yaml
          headersFormat:
            WholeMatch: "${0}"
            GroupByIndex: "${1}"
            NamedGroup: "const ${groupName}"
            JustConst: "just const"
        ```
        You will get the following result:
        ```csv
        "GroupByIndex","JustConst","NamedGroup","WholeMatch"
        "test","just const","42","test 42"
        ```
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

### 4.0.0

+ Updated common to 5.0.0
    + Migration to books-pages
+ Updated read-file-common-core to 2.0.0

### 3.5.2

+ Fixed release version: removed `SNAPSHOT`

### 3.5.1

+ Fixed release workflow


### 3.5.0

+ Update dependencies with vulnerabilities
  + log4j 1.2 is excluded
  + kotlin updated to 1.6.21
+ Parameter `skipBefore` for filtering log messages by timestamp from the file
+ Parameter `syncWithCradle` for timestamp and sequence synchronization with Cradle.
  Enabled by default.

### 3.5.0

+ Updated `kotlin` to 1.6.21
+ Updated `common` to 3.44.0

### 3.4.0

+ Add parameter `timestampZone` which should be used to process the timestamp from the log file

### 3.3.3

+ Update common version from `3.16.1` to `3.37.1`
  + Fixed logging configuration when running as external box and using configurations from k8s

### 3.3.2

+ Update read-file core to `1.2.2`
  + Check file existence when getting the list of files from the directory

### 3.3.1

#### Fixed

+ Bug when we tried to substitute the values from string extracted from the log if it had content like this `${something}`

### 3.3.0

+ Add support for truncated files. `allowFileTruncate` parameter in `common` configuration is added.

### 3.2.0

+ Use different approach for handling new data available in source

### 3.1.0

+ Added the direction regexps option for the alias configuration

### 3.0.0

+ Migrate to a common read-core
