package es.revengenetwork.storage.sql;

import es.revengenetwork.storage.sql.logic.LogicOperand;

/**
 * Generic Dialect for every SQL variant.
 *
 * @author OcZi (Salva)
 */
public interface SQLDialect {

  String selectColumns(String[] columns, String tableName);

  String selectAll(String tableName);

  String selectWhere(String tableName, LogicOperand operand);

  default String selectRow(TableStructure structure) {
    return this.selectRow(structure.tableName(), structure.id());
  }

  String selectRow(String tableName, String id);

  String selectRowWhere(String tableName, String id, String where, String value);

  default String insertInto(TableStructure structure) {
    return this.insertInto(structure.tableName(), structure.columns());
  }

  String insertInto(String tableName, String[] columns);

  String update(String tableName, String[] columns, String idColumn);

  default String deleteRow(TableStructure structure) {
    return this.deleteRow(structure.tableName(), structure.id());
  }

  String deleteRow(String tableName, String idColumn);

  String deleteAllTable(String tableName);

  String createTable(TableStructure structure);
}
