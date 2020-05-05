package com.craftinginterpreters.lox;

import java.util.List;

interface Func {
  Object call(final Interpreter interpreter, final List<Object> arguments, final Token caller);
}

class LoxNative implements LoxCallable {
  final int arity;
  final Func func;

  LoxNative(final int arity, final Func func) {
    this.arity = arity;
    this.func = func;
  }

  public int arity() {
    return arity;
  }

  public Object call(final Interpreter interpreter, final List<Object> arguments, final Token caller) {
    return func.call(interpreter, arguments, caller);
  }

  public String toString() {
    return "<native fn>";
  }
}
