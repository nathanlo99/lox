
package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  class Value {
    public boolean assigned;
    public Object value;

    Value(boolean assigned, Object value) {
      this.assigned = assigned;
      this.value = value;
    }
  }

  final Environment parent;
  private final Map<String, Value> values = new HashMap<>();

  Environment() { parent = null; }
  Environment(Environment parent) { this.parent = parent; }

  Environment ancestor(final int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.parent;
    }
    return environment;
  }

  void define(final String name, final boolean initialized, final Object value) {
    values.put(name, new Value(initialized, value));
  }

  Object get(final Token name) {
    if (values.containsKey(name.lexeme)) {
      final Value value = values.get(name.lexeme);
      if (!value.assigned)
        throw new RuntimeError(name, "Accessing uninitialized variable '" + name.lexeme + "'.");
      return value.value;
    } else if (parent != null) {
      return parent.get(name);
    } else {
      throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
  }

  Object getAt(final int distance, final String name) {
    return getAt(distance, new Token(TokenType.IDENTIFIER, name));
  }
  
  Object getAt(final int distance, final Token name) {
    final Value value = ancestor(distance).values.get(name.lexeme);
    if (!value.assigned)
      throw new RuntimeError(name, "Accessing uninitialized variable '" + name.lexeme + "'.");
    return value.value;
  }

  void assignAt(final int distance, final Token name, final Object value) {
    ancestor(distance).values.put(name.lexeme, new Value(true, value));
  }

  void assign(final Token name, final Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, new Value(true, value));
    } else if (parent != null) {
      parent.assign(name, value);
    } else {
      throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
  }
}
