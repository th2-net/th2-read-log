# Log Reader User Manual 1.00

## Document Information

### Table of contents

{table_contents}

### Introduction

Log reader read text log files, line by line and applying regex expression to each line. Results are sending to RabbitMQ.
Log reader produces **raw messages**. See **RawMessage** type in infra.proto.

### Quick start

#### Configuration

##### Reader confgiuration

Example:
```json
{
  "log-file": "path/to/file.log",
  "regexp": "some*regexp",
  "regexp-groups": [0,2]
}
```

**logFile** - specifying path where log file is located

**regexp** - regular expression to parse string

**regexpGroups** - specifying regex group to be sending

If not specified - will send all matched groups. 

##### Pin declaration

The log reader requires a single pin with _publish_ and _raw_ attributes. The data is published in a raw format. To use it please conect the output pin with another pin that transforms raw data to parsed data. E.g. the **codec** box.

Example:
```yaml
apiVersion: th2.exactpro.com/v1
kind: Th2GenericBox
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

Input string: 6 2015-02-09 16:23:59,455 DEBUG   (FixService(NFT2)    ) - 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=mit123\u00011137=9\u000110=027\u0001
Regex: 8=FIX.+10=.+?
Regex group: 0 
Output: 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=mit123\u00011137=9\u000110=0

#### Example 2

Parse FIX messages only from specified lines

Input string: 6 2015-02-09 16:23:59,455 DEBUG   (FixService(NFT2)    ) - 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=mit123\u00011137=9\u000110=027\u0001
Regex: (FixService.+)(8=FIX.+10=.+?)
Regex group: 2 
Output: 8=FIXT.1.1\u00019=66\u000135=A\u000134=1\u000149=NFT2_FIX1\u000156=FGW\u000198=0\u0001108=10\u0001141=Y\u0001554=mit123\u00011137=9\u000110=0

