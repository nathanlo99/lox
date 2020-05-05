package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  private final Environment closure;
  private final boolean is_initializer;
  private final boolean is_method;
  private final boolean is_static;
  final boolean is_getter;

  LoxFunction(final Stmt.Function declaration, final Environment closure,
              final boolean is_method, final boolean is_initializer, final boolean is_static,
              final boolean is_getter) {
    this.declaration = declaration;
    this.closure = closure;
    this.is_method = is_method;
    this.is_initializer = is_initializer;
    this.is_static = is_static;
    this.is_getter = is_getter;
  }

  LoxFunction bind(final LoxInstance instance) {
    final Environment environment = new Environment(closure);
    environment.define("this", true, instance);
    return new LoxFunction(declaration, environment, is_method, is_initializer, is_static, is_getter);
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(final Interpreter interpreter, final List<Object> arguments, final Token caller) {
    final Environment environment = new Environment(closure);
    final Object _this = is_method ? closure.getAt(0, "this") : null;
    if (is_method) { // Check static
      if (is_static && !(_this instanceof LoxClass))
        throw new RuntimeError(caller, "Cannot call static function from instance.");
      else if (!is_static && _this instanceof LoxClass)
        throw new RuntimeError(caller, "Cannot call method from non-instance (class).");
    }
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, true, arguments.get(i));
    }
    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (final Return returnValue) {
      return is_initializer ? _this : returnValue.value;
    }
    if (is_initializer) return _this;
    return null;
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}
