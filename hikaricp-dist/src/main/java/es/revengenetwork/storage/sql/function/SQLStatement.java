package es.revengenetwork.storage.sql.function;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface SQLStatement {

  void accept(PreparedStatement statement) throws SQLException;
}
