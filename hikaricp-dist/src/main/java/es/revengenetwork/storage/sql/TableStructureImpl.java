package es.revengenetwork.storage.sql;

import com.google.gson.JsonObject;
import java.util.Map;

public record TableStructureImpl(String id, String tableName, Map<String, String> content) implements TableStructure {

  @Override
  public boolean matchContent(final JsonObject model) {
    return this.content.keySet()
             .stream()
             .allMatch(model::has);
  }
}
