    
# Raven

Raven is the Java client for [Sentry](https://www.getsentry.com/).  
It relies on the most common java logging libraries to capture and convert logs
before sending the info to a Sentry instance.

## Supported logging frameworks

 - [`java.util.logging`](http://docs.oracle.com/javase/7/docs/technotes/guides/logging/index.html)
 support is provided by the main project, [raven](raven).
 - [log4j](https://logging.apache.org/log4j/1.2/) support is provided in [raven-log4j](raven-log4j).
 - [log4j2](https://logging.apache.org/log4j/2.x/) can be used with [raven-log4j2](raven-log4j2).
 - [logback](http://logback.qos.ch/) support is available in [raven-logback](raven-logback).

While it's **strongly recommended** to use one of the supported logging
frameworks to capture and send messages to Sentry, it is possible to communicate
with the Sentry instance manually using only [raven](raven).

## Sentry Protocol and Raven versions
Since Raven 2.0, the major version of raven matches the version of the Sentry protocol.

|  Raven version  | Protocol version | Sentry version |
| --------------- | ---------------- | -------------- |
| Raven 2.x (old) | V2               | >= 2.0         |
| Raven 3.x (old) | V3               | >= 5.1         |
| Raven 4.x       | V4               | >= 6.0         |
| Raven 5.x       | V5               | >= 6.4         |


Each release of Sentry supports the last two versions of the protocol
(i.e. Sentry 6.4.2 supports both the protocol V4 as well as V5), for this reason, only
the two last stable versions of Raven are actively maintained.

### Snapshot versions
While the stable versions of raven are available on the
[central Maven Repository](https://search.maven.org), newer (but less stable)
versions (AKA snapshots) are available in Sonatype's  OSS snapshot repository.

To use it with maven, add the following repository:

```xml
<repository>
    <id>sonatype-nexus-snapshots</id>
    <name>Sonatype Nexus Snapshots</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <releases>
        <enabled>false</enabled>
    </releases>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```
