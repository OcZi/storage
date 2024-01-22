package es.revengenetwork.storage.sql.logic;

import java.util.LinkedHashMap;
import java.util.Map;

public class LogicOperandImpl implements LogicOperand {

  private final Map<String, String> builder;
  private final String base;

  public LogicOperandImpl(final String base) {
    this.base = base;
    this.builder = new LinkedHashMap<>();
  }

  @Override
  public LogicOperand and(final String str) {
    this.builder.put(str + " = ?", LogicOperand.AND);
    return this;
  }

  @Override
  public LogicOperand or(final String str) {
    this.builder.put(str + " = ?", LogicOperand.OR);
    return this;
  }

  @Override
  public String build() {
    final StringBuilder line = new StringBuilder();
    this.builder.forEach((key, value) -> line.append(value).append(key));
    return this.base + line;
  }
}
