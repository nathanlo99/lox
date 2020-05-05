
package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
  private LoxClass _class;
  private final Map<String, Object> fields = new HashMap<>();

  LoxInstance(final LoxClass _class) {
    this._class = _class;
  }

  Object get(final Token name) {
    if (fields.containsKey(name.lexeme)) {
      return fields.get(name.lexeme);
    }
    final LoxFunction method = _class.findMethod(name.lexeme);
    if (method != null) return method.bind(this);

    throw new RuntimeError(name, "Undefined property '" + name.lexeme + "' for class " + _class.name);
  }

  void set(final Token name, final Object value) {
    if (!Lox.allowFieldCreation && !fields.containsKey(name.lexeme)) {
      throw new RuntimeError(name, "Undefined property '" + name.lexeme + "' assigned to.");
    }
    fields.put(name.lexeme, value);
  }

  @Override
  public String toString() {
    return _class.name + "@" + hashCode();
  }
}
