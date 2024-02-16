package es.revengenetwork.storage.hikaricp;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zaxxer.hikari.HikariDataSource;
import es.revengenetwork.storage.codec.ModelDeserializer;
import es.revengenetwork.storage.codec.ModelSerializer;
import es.revengenetwork.storage.model.Model;
import es.revengenetwork.storage.repository.AbstractAsyncModelRepository;
import es.revengenetwork.storage.sql.SQLDialect;
import es.revengenetwork.storage.sql.TableStructure;
import es.revengenetwork.storage.sql.function.SQLResultSet;
import es.revengenetwork.storage.sql.function.SQLStatement;
import es.revengenetwork.storage.sql.logic.LogicOperand;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HikariCPModelRepository<ModelType extends Model> extends AbstractAsyncModelRepository<ModelType> {
  private final ModelSerializer<ModelType, JsonObject> serializer;
  private final ModelDeserializer<ModelType, JsonObject> deserializer;
  private final Gson gson; // I hate it

  private final HikariDataSource dataSource;
  private final SQLDialect dialect;

  private final TableStructure table;

  public HikariCPModelRepository(
    final @NotNull Executor executor,
    final ModelSerializer<ModelType, JsonObject> serializer,
    final ModelDeserializer<ModelType, JsonObject> deserializer,
    final Gson gson,
    final HikariDataSource dataSource,
    final SQLDialect dialect,
    final TableStructure table
  ) {
    super(executor);
    this.serializer = serializer;
    this.deserializer = deserializer;
    this.gson = gson;
    this.dialect = dialect;
    this.dataSource = dataSource;
    this.table = table;
  }

  @Override
  public @Nullable ModelType findSync(final @NotNull String id) {
    final JsonObject serialized = this.queryModel(
      this.dialect.selectRow(this.table),
      id);
    return serialized.entrySet().isEmpty() ? null : this.deserializer.deserialize(serialized);
  }

  @Override
  public <C extends Collection<ModelType>> @Nullable C findSync(
    final @NotNull String field,
    final @NotNull String value,
    final @NotNull Function<Integer, C> factory
  ) {
    final AtomicReference<C> reference = new AtomicReference<>();
    this.query(
      this.dialect.selectRow(this.table.tableName(), field),
      statement -> statement.setObject(1, value),
      this.deserializeAll(reference, factory));
    return reference.get();
  }

  @Override
  public @Nullable Collection<String> findIdsSync() {
    final List<String> allIds = new ArrayList<>();
    this.query(this.dialect.selectColumns(new String[]{this.table.id()}, this.table.tableName()), statement -> {},
          resultSet -> {
            while (resultSet.next()) {
              allIds.add(
                String.valueOf(
                  resultSet.getObject(this.table.id())
                )
              );
            }
          });
    return allIds;
  }

  @Override
  public <C extends Collection<ModelType>> @Nullable C findAllSync(
    final @NotNull Consumer<ModelType> postLoadAction,
    final @NotNull Function<Integer, C> factory
  ) {
    final AtomicReference<C> reference = new AtomicReference<>();
    this.query(
      this.dialect.selectAll(this.table.tableName()),
      statement -> {},
      this.deserializeAll(reference, factory));
    return reference.get();
  }

  private <C extends Collection<ModelType>> SQLResultSet deserializeAll(final AtomicReference<C> reference,
                                                                        final @NotNull Function<Integer, C> factory) {
    return resultSet -> {
      final C collection = factory.apply(resultSet.getFetchSize());
      while (resultSet.next()) {
        final JsonObject object = new JsonObject();
        this.deserializeJson(resultSet, object);

        collection.add(this.deserializer.deserialize(object));
      }
      reference.set(collection);
    };
  }

  @Override
  public boolean existsSync(final @NotNull String id) {
    final AtomicBoolean exists = new AtomicBoolean();
    this.query(this.dialect.selectRow(this.table),
          statement -> statement.setObject(1, id),
          resultSet -> exists.set(resultSet.next())
    );
    return exists.get();
  }

  @Override
  public ModelType saveSync(final @NotNull ModelType model) {
    this.runUpdate(
      this.dialect.insertInto(this.table),
      statement -> this.serialize(statement, model));
    return model;
  }

  @Override
  public boolean deleteSync(final @NotNull String id) {
    return this.runUpdate(
      this.dialect.deleteRow(this.table),
      statement -> statement.setObject(1, id)) > 0;
  }

  private void query(
    final String expression,
    final SQLStatement statementConsumer,
    final SQLResultSet resultSetConsumer
  ) {
    try (
      var connection = this.dataSource.getConnection();
      var statement = connection.prepareStatement(expression)
    ) {

      statementConsumer.accept(statement);
      try (var result = statement.executeQuery()) {
        resultSetConsumer.accept(result);
      }
    } catch (final SQLException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private JsonObject queryModel(final String expression, final String id) {
    final JsonObject jsonObject = new JsonObject();
    this.query(
      expression,
      statement -> statement.setObject(1, id),
      resultSet -> {
        int i = 0;
        while (resultSet.next()) {
          if (i > 0) {
            throw new SQLException("Unexpected behaviour: more than a row returned in a single query row");
          }

          this.deserializeJson(resultSet, jsonObject);
          ++i;
        }
      });
    return jsonObject;
  }

  private void deserializeJson(final ResultSet result, final JsonObject serialized) throws SQLException {
    final String[] columns = this.table.columns();
    final int columnCount = result.getMetaData().getColumnCount();
    for (int i = 0; i < columnCount; i++) {
      final Object object = result.getObject(i + 1);
      final JsonElement jsonTree = this.gson.toJsonTree(object);
      serialized.add(
        columns[i].toLowerCase(), jsonTree
      );
    }
  }

  private void serialize(final PreparedStatement statement, final ModelType type) throws SQLException {
    final String[] columns = this.table.columns();
    final JsonObject serialize = this.serializer.serialize(type);
    for (int i = 0; i < columns.length; i++) {
      final JsonElement x = serialize.get(columns[i].toLowerCase());
      statement.setObject(i + 1, x.getAsString());
    }
  }

  private int runUpdate(final String expression, final SQLStatement consumer) {
    try (
      var connection = this.dataSource.getConnection();
      var statement = connection.prepareStatement(expression)
    ) {
      consumer.accept(statement);
      return statement.executeUpdate();
    } catch (final SQLException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
