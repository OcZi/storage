package es.revengenetwork.storage.sql;

import es.revengenetwork.storage.sql.logic.LogicOperand;

public class MySQLDialect implements SQLDialect {

  @Override
  public String selectColumns(final String[] columns, final String tableName) {
    return "SELECT %s FROM %s;".formatted(String.join(", ", columns), tableName);
  }

  @Override
  public String selectAll(final String tableName) {
    return "SELECT * FROM %s;".formatted(tableName);
  }

  @Override
  public String selectWhere(final String tableName, final LogicOperand operand) {
    return "SELECT * FROM %s WHERE %s;".formatted(tableName, operand.build());
  }

  @Override
  public String selectRow(final String tableName, final String idColumn) {
    return "SELECT * FROM %s WHERE %s = ?;".formatted(tableName, idColumn);
  }

  @Override
  public String selectRowWhere(final String tableName, final String id, final String where, final String value) {
    return "SELECT * FROM %s WHERE %s = ?, %s = ?;";
  }

  @Override
  public String insertInto(final String tableName, final String[] columns) {
    return "INSERT INTO %s (%s) values (%s);"
             .formatted(tableName,
                        String.join(", ", columns),
                        this.repeat(", ", "?", columns.length));
  }

  @Override
  public String update(final String tableName, final String[] columns, final String idColumn) {
    return "UPDATE %s SET %s WHERE %s = ?;"
             .formatted(tableName,
                        String.join(" = ?, ", columns),
                        idColumn);
  }

  @Override
  public String deleteRow(final String tableName, final String idColumn) {
    return "DELETE * FROM %s WHERE %s = ?;".formatted(tableName, idColumn);
  }

  @Override
  public String deleteAllTable(final String tableName) {
    return "DELETE FROM %s;".formatted(tableName);
  }

  @Override
  public String createTable(final TableStructure structure) {
    return "CREATE IF NOT EXISTS %s VALUES (%s);"
             .formatted(structure.tableName(), structure.tableValues());
  }

  private String repeat(final String delimiter, final String str, final int count) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < count; i++) {
      if (!builder.isEmpty()) {
        builder.append(delimiter);
      }

      builder.append(str);
    }

    return builder.toString();
  }
}
