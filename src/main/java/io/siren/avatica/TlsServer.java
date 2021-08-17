/*
 * (C) Copyright 2021 Sindice LTD (https://siren.io/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.siren.avatica;

import org.apache.calcite.avatica.jdbc.JdbcMeta;
import org.apache.calcite.avatica.remote.Driver.Serialization;
import org.apache.calcite.avatica.remote.LocalService;
import org.apache.calcite.avatica.server.HttpServer;
import org.apache.calcite.avatica.server.ServerCustomizer;
import org.apache.calcite.avatica.util.Unsafe;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.Locale;

/**
 * An Avatica server for arbitrary JDBC drivers with TLS support.
 */
public class TlsServer {
  private static final Logger LOG = LoggerFactory.getLogger(TlsServer.class);

  @Parameter(names = {"-u", "--url"}, required = true, description = "JDBC driver url for the server")
  private String url;

  @Parameter(names = {"-p", "--port"}, description = "Port the server should bind to")
  private int port = 0;

  @Parameter(names = {"--host"}, description = "IP address the server should bind to")
  private String host = "0.0.0.0";

  @Parameter(names = {"--keystore"}, description = "The path to a keystore file containing the server certificate")
  private String keystore;

  @Parameter(names = {"--keystorePassword"}, description = "The keystore password")
  private String keystorePassword;

  @Parameter(names = {"-s", "--serialization"},
      description = "Serialization method to use", converter = SerializationConverter.class)
  private Serialization serialization = Serialization.PROTOBUF;

  @Parameter(names = {"-h", "-help", "--help"},help = true, description = "Print the help message")
  private boolean help = false;

  private HttpServer server;

  public void start() {
    try {
      JdbcMeta meta = new JdbcMeta(url);
      LocalService service = new LocalService(meta);

      HttpServer.Builder<Server> builder = new HttpServer.Builder<Server>()
          .withHandler(service, serialization)
          .withPort(port);

      if (keystore != null) {
        File keystoreFile = new File(keystore);
        if (!(keystoreFile.exists() && keystoreFile.isFile())) {
          LOG.error("keystore file not found");
          Unsafe.systemExit(1);
          return;
        }
        if (keystorePassword == null) {
          keystorePassword = "";
        }
        builder.withTLS(keystoreFile, keystorePassword, keystoreFile, keystorePassword);
      }

      if (!host.equals("0.0.0.0")) {
        builder.withServerCustomizers(Collections.singletonList(server -> {
          Connector[] connectors = server.getConnectors();
          if (connectors.length != 1) {
            LOG.error("The number of Jetty server connectors is not 1, please check your configuration");
            Unsafe.systemExit(1);
            return;
          }
          try {
            ServerConnector serverConnector = (ServerConnector) connectors[0];
            serverConnector.setHost(host);
          } catch (Exception e) {
            LOG.error("Could not set the server to listen on host {}, please check that the address exists.", host);
            Unsafe.systemExit(1);
            return;
          }
        }), Server.class);
      }
      this.server = builder.build();

      server.start();

      LOG.info("Started Avatica server on {}:{} with serialization {}", host, server.getPort(), serialization);
    } catch (Exception e) {
      LOG.error("Failed to start Avatica server", e);
      Unsafe.systemExit(1);
    }
  }

  public void stop() {
    if (null != server) {
      server.stop();
      server = null;
    }
  }

  public void join() throws InterruptedException {
    server.join();
  }

  public static void main(String[] args) {
    final TlsServer server = new TlsServer();
    JCommander jc = new JCommander(server);
    jc.parse(args);
    if (server.help) {
      jc.usage();
      Unsafe.systemExit(1);
      return;
    }

    server.start();

    Runtime.getRuntime().addShutdownHook(
        new Thread(() -> {
          LOG.info("Stopping server");
          server.stop();
          LOG.info("Server stopped");
        }));

    try {
      server.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Converter from String to Serialization. Must be public for JCommander.
   */
  public static class SerializationConverter implements IStringConverter<Serialization> {
    public Serialization convert(String value) {
      return Serialization.valueOf(value.toUpperCase(Locale.ROOT));
    }
  }

}
