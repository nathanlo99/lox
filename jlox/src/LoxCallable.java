package com.craftinginterpreters.lox;

import java.util.List;

interface LoxCallable {
  int arity();
  Object call(final Interpreter interpreter, final List<Object> arguments);
}
