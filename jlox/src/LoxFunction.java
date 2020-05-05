package com.craftinginterpreters.lox;

import java.util.List;

enum LoxFunctionType {
  FUNCTION,
  INITIALIZER,
  STATIC_GETTER,
  STATIC_METHOD,
  INSTANCE_GETTER,
  INSTANCE_METHOD
}

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  private final Environment closure;
  final LoxFunctionType type;

  LoxFunction(final Stmt.Function declaration, final Environment closure, final LoxFunctionType type) {
    this.declaration = declaration;
    this.closure = closure;
    this.type = type;
  }

  LoxFunction bind(final LoxInstance instance) {
    final Environment environment = new Environment(closure);
    environment.define("this", true, instance);
    return new LoxFunction(declaration, environment, type);
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(final Interpreter interpreter, final List<Object> arguments, final Token caller) {
    final Environment environment = new Environment(closure);
    final Object _this = type == LoxFunctionType.FUNCTION ? null : closure.getAt(0, "this");
    if (type != LoxFunctionType.FUNCTION) { // Check static rules
      final boolean is_static = (type == LoxFunctionType.STATIC_METHOD) | (type == LoxFunctionType.STATIC_GETTER);
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
      return type == LoxFunctionType.INITIALIZER ? _this : returnValue.value;
    }
    if (type == LoxFunctionType.INITIALIZER) return _this;
    return null;
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}
