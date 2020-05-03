package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  private final Environment closure;
  LoxFunction(final Stmt.Function declaration, final Environment closure) {
    this.declaration = declaration;
    this.closure = closure;
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
      return returnValue.value;
    }
    return null;
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}
