
# Connections and protocols
To communicate with Sentry two important elements must be used.  
The connection which will allow Raven to send information to Sentry.
The protocol which will be used to communicate between the two.

## Connection to Sentry
Sentry supports by default two ways of communication, HTTP(s) and UDP.  
Both of them are handled by Raven with some additional options to get
around some security issues in Java.

While UDP is fully supported, it is recommended to use HTTPS to both ensure
that the content sent to Sentry hasn't been tampered and that the message has
been received by the Sentry server.

### HTTP
The most common way send events to Sentry is through HTTP, this can be done by
using a DSN of this form:

    http://public:private@host:port/1

If not specified, the port will default to `80`.

### HTTPS
It is possible to use an encrypted connection to Sentry using HTTPS:

    https://public:private@host:port/1

If not specified, the port will default to `443`.

### HTTPS (naive)
If the certificate used over HTTPS is a wildcard certificate (which is not
handled by every version of Java), and the certificate hasn't been added to the
truststore, it is possible to specify to the client that it needs to be
naive and ignore the hostname verification:

    naive+https://public:private@host:port/1

### UDP
It is also possible to contact Sentry with UDP:

    udp://public:private@host:port/1

If not specified, the port will default to `9001`.

While being faster because there is no TCP and HTTP overhead, UDP doesn't
guarantee the integrity of the message nor that it has been received.

### Custom connections
Raven allows the creation of new means of connection, allowing the use of
middlewares or other tools.

See [custom connection](custom/connection.html).

## Protocol
Sentry only support one protocol so far which is JSON formatted messages.

It is possible to deflate the JSON content as well, in which case it will
be sent as a base64 encoded version of the compressed message.

Those options can be set with a few [options](options.html).
