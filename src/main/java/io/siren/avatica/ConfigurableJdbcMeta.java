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

import java.sql.SQLException;

/**
 * An extension of JdbcMeta that allows to override JDBC connection properties.
 */
public class ConfigurableJdbcMeta extends JdbcMeta {

  private final ConnectionProperties properties;

  /**
   * Creates a new ConfigurableJdbcMeta for a JDBC url with the specified extra properties.
   * @param url A valid JDBC url.
   * @param properties An instance of ConnectionProperties that will be merged with the existing connection properties.
   * @throws SQLException
   */
  public ConfigurableJdbcMeta(String url, ConnectionProperties properties) throws SQLException {
    super(url);
    this.properties = properties;
  }

  @Override
  public ConnectionProperties connectionSync(ConnectionHandle ch, ConnectionProperties connProps) {
    return super.connectionSync(ch, connProps.merge(this.properties));
  }

}
