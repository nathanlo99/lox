
package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

class LoxInstance {
  protected LoxClass _class;
  private final Map<String, Object> fields = new HashMap<>();
  protected final Interpreter interpreter;

  LoxInstance(final LoxClass _class, final Interpreter interpreter) {
    this._class = _class;
    this.interpreter = interpreter;
  }

  Object get(final Token name) {
    if (name.lexeme.equals("__class__")) return (LoxInstance)_class;
    if (name.lexeme.equals("name") && this instanceof LoxClass) return _class.name;
    if (fields.containsKey(name.lexeme)) {
      return fields.get(name.lexeme);
    }
    final LoxFunction method = _class.findMethod(name.lexeme);
    if (method != null) {
      final LoxFunction bound_method = method.bind(this);
      if (method.type == LoxFunctionType.INSTANCE_GETTER || method.type == LoxFunctionType.STATIC_GETTER)
        return bound_method.call(_class.interpreter, new ArrayList<Object>(), name);
      return bound_method;
    }
    // Lastly, try the superclass static fields
    if (_class.superclass != null) {
      final Object result = _class.superclass.get(name);
      if (result != null) return result;
    }
    throw new RuntimeError(name, "Undefined property '" + name.lexeme + "' for class " + _class.name);
  }

  void set(final Token name, final Object value) {
    if (!Lox.allowFieldCreation && !fields.containsKey(name.lexeme)) {
      throw new RuntimeError(name, "Undefined property '" + name.lexeme + "' assigned to.");
    }
    final LoxFunction method = _class.findMethod(name.lexeme);
    if (method != null &&
       (method.type == LoxFunctionType.INSTANCE_GETTER || method.type == LoxFunctionType.STATIC_GETTER)) {
      throw new RuntimeError(name, "Cannot assign to getter.");
    }
    fields.put(name.lexeme, value);
  }

  @Override
  public String toString() {
    return _class.name + "@" + hashCode();
  }
}
