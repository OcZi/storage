package es.revengenetwork.storage.sql;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public interface TableStructure {

  static TableStructure newTable(final String name, final String... lines) {
    final Map<String, String> columns = new LinkedHashMap<>();
    String idColumn = null;
    for (final String line : lines) {
      final int split = line.indexOf(" ");
      final String column = line.substring(0, split);
      final String tags = line.substring(split + 1);
      if (idColumn == null && tags.toLowerCase()
                                .contains("primary key")) {
        idColumn = column;
      }

      columns.put(column, tags);
    }

    return new TableStructureImpl(Objects.requireNonNull(idColumn, "Primary key not found"), name, columns);
  }

  String id();

  String tableName();

  Map<String, String> content();

  default String tableValues() {
    return this.content().entrySet()
             .stream()
             .map(entry -> entry.getKey() + " " + entry.getValue())
             .collect(Collectors.joining(", "));
  }

  default String[] columns() {
    return this.content().keySet()
             .toArray(new String[0]);
  }

  default int size() {
    return this.content().size();
  }
}
