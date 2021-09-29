# Avatica TLS Server

The Avatica TLS Server is a variation of the [Avatica Standalone](https://calcite.apache.org/avatica/docs/) server that exposes the following extra options:

- `keystore`: the path to a Java keystore or p12 bundle that contains a key and a certificate for the server.
- `keystorePassword`: the password of the keystore.
- `host`: the IP address that the Avatica HTTP server will bind to (bound to all the IP addresses by default).
- `autoCommit`: allows to force the auto commit mode of JDBC datasource connections.
- `readOnly`: allows to force the read only mode of JDBC datasource connections.

If a keystore is set, TLS will be enabled in the embedded HTTP server, otherwise the server will not enable any form of encrypted transport.

For the full list of options, including the ones provided by the original Avatica Standalone server, see the **Usage** section below.

## Usage

### Docker

Docker images are available at https://hub.docker.com/repository/docker/sirensolutions/avatica-tls-server .

In order to use Avatica TLS Server with a JDBC datasource, you will need to have the JDBC driver for the datasource in your classpath.

Any jar in the `/home/avatica/jdbc` directory inside the image will be included in the classpath automatically, so you can either:

- Bind mount a directory containing the JDBC driver and its dependencies to `/home/avatica/jdbc`.
- Create a new Docker image that inherits the `sirensolutions/avatica-tls-server` images and add the JDBC driver and its dependencies to `/home/avatica/jdbc`.

If you are creating a new Docker image, ensure that the `avatica` user can read the files added to `/home/avatica/jdbc`; a minimal example for PostgreSQL follows:

```
FROM sirensolutions/avatica-tls-server:latest

# Download the PostgreSQL JDBC driver to /home/avatica/jdbc directory
# so that it is included in the classpath and set the owner
# to the avatica user
ADD --chown=avatica https://jdbc.postgresql.org/download/postgresql-42.2.23.jar /home/avatica/jdbc
```

#### Docker container options

The entry point of the Docker image accepts the following options:

- `s`: the [serialization format](https://calcite.apache.org/avatica/docs/client_reference.html#serialization) of Avatica requests; valid values are `json` and `protobuf`, defaults to `protobuf.`
- `u`: the JDBC URL of the datasource Avatica is connected to (e.g. `jdbc:postgresql://postgres:5432/test`). Note that credentials must not be set in the URL, as they will be specified by Avatica clients and passed through by the Avatica server.
- `keystore`: the path to a Java keystore or p12 bundle that contains a key and a certificate for the server.
- `keystorePassword`: the password of the keystore.
- `autoCommit`: allows to force the auto commit mode of JDBC datasource connections (unset by default, can be `true` or `false`).
- `readOnly`: allows to force the read only mode of JDBC datasource connections (unset by default, can be `true` or `false`).

#### Example

The `example` directory contains a `docker-compose.yml` file that starts an Avatica TLS server image and a PostgreSQL image.

To run the example you will need the following software on your system:

- A Java distribution from https://jdk.java.net/
- [Docker](https://docker.com)

In order to quickly test the image you can follow these steps:

1. Clone this repository.
2. Go to the `example` directory.
3. Create a new directory named `pki`.
4. Create a new self-signed certificate and its key by executing `keytool` as follows; set the password to `password` when asked:

```bash
keytool -genkey \
  -keystore pki/avatica.p12 -storetype PKCS12 \
  -alias avatica \
  -keyalg RSA -keysize 2048 -sigalg SHA256withRSA \
  -validity 3650 \
  -dname CN=localhost -ext san=dns:localhost
```

  If the process is successful, you should see a file named `avatica.p12` in `example/pki` directory. This directory will be bind mounted by Docker compose at `/home/avatica/pki` inside the Avatica server container.
  
4. Execute `docker-compose up` to start the Docker containers.

You should now be able to test that Avatica is connected to the PostgreSQL database by sending a raw connection opening request with curl:

```bash
curl -k https://localhost:8765 -H "Content-Type: application/json" -d '{
  "request": "openConnection",
  "connectionId": "123",
  "info": {
    "user": "test",
    "password": "password"
  }
}'
```

If the connection is successful, you will get back a JSON response containing the same value as `request` in the `response` field, for example:

```json
{
  "response": "openConnection",
  "rpcMetadata": {
    "response": "rpcMetadata",
    ...
  }
}
```

You can then stop docker compose by pressing `CTRL+C` and run `docker-compose down` to cleanup the example.

### Java

In order to use the Avatica TLS Server without Docker you will need Java 11 or later from https://java.jdk.net installed on your system.

Avatica TLS Server is distributed as a standalone jar file.

After downloading a release and the JDBC driver of the target system to the same directory as the jar file, it can be started as follows:

```bash
java -cp "./*" "io.siren.avatica.TlsServer" <OPTIONS>
```

The supported options are as follows:

- `u`: the JDBC URL of the datasource Avatica is connected to (e.g. `jdbc:postgresql://postgres:5432/test`). Note that credentials must not be set in the URL, as they will be specified by Avatica clients and passed through by the Avatica server.
- `keystore`: the path to a Java keystore or p12 bundle that contains a key and a certificate for the server.
- `keystorePassword`: the password of the keystore.
- `s`: the [serialization format](https://calcite.apache.org/avatica/docs/client_reference.html#serialization) of Avatica requests; valid values are `json` and `protobuf`, defaults to `protobuf.`
- `host`: the IP address that the Avatica server will bind to (default to all addresses in the system).
- `p`: the port that the Avatica server will bind to (defaults to `8765`).
- `autoCommit`: allows to force the auto commit mode of JDBC datasource connections (unset by default, can be `true` or `false`).
- `readOnly`: allows to force the read only mode of JDBC datasource connections (unset by default, can be `true` or `false`).

For example, assuming that you want to connect to the `test` database of a PostgreSQL server already running at `localhost:5432`, you could follow these steps to start an Avatica TLS Server:

1. Create a new directory named `avatica`
2. Download the Avatica TLS server jar and the [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/download.html) to the directory.
3. Create a new directory named `pki`.
4. Create a new self-signed certificate and its key by executing `keytool` as follows; set the password to `password` when asked:

```bash
keytool -genkey \
  -keystore pki/avatica.p12 -storetype PKCS12 \
  -alias avatica \
  -keyalg RSA -keysize 2048 -sigalg SHA256withRSA \
  -validity 3650 \
  -dname CN=localhost -ext san=dns:localhost
```

  If the process is successful, you should see a file named `avatica.p12` in `example/pki` directory. This directory will be bind mounted by Docker compose at `/home/avatica/pki` inside the Avatica server container.
 
5. Start Avatica server as follows:

```bash
java -cp "./*" "io.siren.avatica.TlsServer" \
  --host 127.0.0.1 \
  --keystore pki/avatica.p12 \
  --keystorePassword password \
  -s json \
  -u "jdbc:postgresql://localhost:5432/test"
```

You should now be able to test that Avatica is connected to the PostgreSQL database by sending a raw connection opening request with curl:

```bash
curl -k https://localhost:8765 -H "Content-Type: application/json" -d '{
  "request": "openConnection",
  "connectionId": "123",
  "info": {
    "user": "test",
    "password": "password"
  }
}'
```

If the connection is successful, you will get back a JSON response containing the same value as `request` in the `response` field, for example:

```json
{
  "response": "openConnection",
  "rpcMetadata": {
    "response": "rpcMetadata",
    ...
  }
}
```

You can then kill Avatica pressing `CTRL+C`.

## Creating a truststore file from a p12 bundle

When connecting to the Avatica TLS server using the Avatica JDBC driver, you will need to specify the following extra properties:

- `TRUSTSTORE`: the path to a Java keystore that contains a copy of the CA certificate that can validate the server certificate (or a copy of the self-signed certificate if no CA was used).
- `TRUSTSTORE_PASSWORD`: the password of the keystore.

You might also need to set `HOSTNAME_VERIFICATION` to `NONE` if the certificate of the Avatica TLS server does not match the hostname of the machine the server is running on.

If you created a self-signed certificate like in the examples provided in this document, you can create a truststore as follows:

1. Export the certificate from the p12 bundle file, e.g. :

```bash
openssl pkcs12 -in avatica.p12 -clcerts -nokeys -out avatica.pem
```

2. Create a truststore containing the exported certificate:

```bash
keytool -import -file avatica.pem -alias ca -keystore truststore.jks
```

The `TRUSTSTORE` JDBC property can then be set to point to the `truststore.jks` file.

## License

See the `LICENSE` file.
