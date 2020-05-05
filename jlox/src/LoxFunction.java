package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  private final Environment closure;
  private final boolean is_initializer;

  LoxFunction(final Stmt.Function declaration, final Environment closure, boolean is_initializer) {
    this.declaration = declaration;
    this.closure = closure;
    this.is_initializer = is_initializer;
  }

  LoxFunction bind(final LoxInstance instance) {
    final Environment environment = new Environment(closure);
    environment.define("this", true, instance);
    return new LoxFunction(declaration, environment, is_initializer);
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(final Interpreter interpreter, final List<Object> arguments) {
    final Environment environment = new Environment(closure);
    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, true, arguments.get(i));
    }
    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (final Return returnValue) {
      return is_initializer ? closure.getAt(0, "this") : returnValue.value;
    }
    if (is_initializer) return closure.getAt(0, "this");
    return null;
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}
