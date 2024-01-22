package es.revengenetwork.storage.sql.function;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SQLResultSet {

  void accept(ResultSet resultSet) throws SQLException;
}
