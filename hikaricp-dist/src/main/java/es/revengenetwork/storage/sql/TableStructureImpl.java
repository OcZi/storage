package es.revengenetwork.storage.sql;

import java.util.Map;

public record TableStructureImpl(String id, String tableName, Map<String, String> content) implements TableStructure {}
