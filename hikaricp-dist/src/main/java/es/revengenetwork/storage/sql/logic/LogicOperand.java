package es.revengenetwork.storage.sql.logic;

/**
 * Very basic logic operand builder for SQL sentences.
 */
public interface LogicOperand {
  // Lets be generic
  String AND = " AND ";
  String OR = " OR ";

  static LogicOperand of(String base) {
    return new LogicOperandImpl(base);
  }

  LogicOperand and(String str);

  LogicOperand or(String str);

  String build();
}
