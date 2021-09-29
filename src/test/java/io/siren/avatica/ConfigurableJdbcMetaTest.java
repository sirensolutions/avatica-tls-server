package io.siren.avatica;
import org.apache.calcite.avatica.ConnectionPropertiesImpl;
import org.apache.calcite.avatica.Meta;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConfigurableJdbcMetaTest {

  final static String URL = "jdbc:avatica:noop";

  Connection getReferenceConnection() throws SQLException {
    final Connection connection = DriverManager.getConnection(URL);
    Assert.assertFalse(connection.isReadOnly());
    Assert.assertFalse(connection.getAutoCommit());
    return connection;
  }

  @Test
  public void emptyProperties() throws SQLException {
    final Meta.ConnectionProperties empty = new ConnectionPropertiesImpl();
    ConfigurableJdbcMeta meta = new ConfigurableJdbcMeta(URL, empty);
    Meta.ConnectionHandle handle = new Meta.ConnectionHandle("empty");

    try (final Connection reference = getReferenceConnection()) {
      meta.openConnection(handle, null);
      final Meta.ConnectionProperties synced = meta.connectionSync(handle, empty);
      Assert.assertEquals(reference.getAutoCommit(), synced.isAutoCommit());
      Assert.assertEquals(reference.isReadOnly(), synced.isReadOnly());
      meta.closeConnection(handle);
    }
  }

  @Test
  public void overrides() throws SQLException {
    final Meta.ConnectionProperties overrides = new ConnectionPropertiesImpl();
    ConfigurableJdbcMeta meta = new ConfigurableJdbcMeta(URL, overrides);
    Meta.ConnectionHandle handle = new Meta.ConnectionHandle("override");
    overrides.setAutoCommit(true);
    overrides.setReadOnly(true);

    try (final Connection reference = getReferenceConnection()) {
      meta.openConnection(handle, null);
      final Meta.ConnectionProperties synced = meta.connectionSync(handle, overrides);
      Assert.assertNotEquals(reference.getAutoCommit(), synced.isAutoCommit());
      Assert.assertNotEquals(reference.isReadOnly(), synced.isReadOnly());
      Assert.assertTrue(synced.isAutoCommit());
      Assert.assertTrue(synced.isReadOnly());
      meta.closeConnection(handle);
    }
  }

}
