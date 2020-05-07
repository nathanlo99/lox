package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

interface Func {
  Object call(final Interpreter interpreter, final List<Object> arguments, final Token caller);
}

class LoxNative implements LoxCallable {
  final String name;
  final int arity;
  final Func func;

  LoxNative(final String name, final int arity, final Func func) {
    this.name = name;
    this.arity = arity;
    this.func = func;
  }

  @Override
  public int arity() {
    return arity;
  }

  @Override
  public Object call(final Interpreter interpreter, final List<Object> arguments, final Token caller) {
    if (arguments.size() != this.arity)
      throw new RuntimeError(caller, "Wrong number of arguments, expected " + arity + ", got " + arguments.size());
    return func.call(interpreter, arguments, caller);
  }

  @Override
  public String toString() {
    return "<native fn " + name + ">";
  }

  // Below: declare native functions
  public static List<LoxNative> getNativeFunctions() {
    ArrayList<LoxNative> result = new ArrayList<>();
    result.add(new LoxNative("clock", 0, (interpreter, arguments, caller) -> {
      return (double)System.currentTimeMillis() / 1000.0;
    }));

    result.add(new LoxNative("print", 1, (interpreter, arguments, caller) -> {
      System.out.print(Interpreter.stringify(arguments.get(0)));
      return null;
    }));
    result.add(new LoxNative("println", 1, (interpreter, arguments, caller) -> {
      System.out.println(Interpreter.stringify(arguments.get(0)));
      return null;
    }));

    result.add(new LoxNative("random", 2, (interpreter, arguments, caller) -> {
      final double a = (Double)arguments.get(0), b = (Double)arguments.get(1);
      return Math.random() * (b - a) + a;
    }));

    result.add(new LoxNative("assert", 1, (interpreter, arguments, caller) -> {
      if (!Interpreter.isTruthy(arguments.get(0)))
        throw new RuntimeError(caller, "Assertion failed");
      return null;
    }));

    result.add(new LoxNative("nextLine", 0, (interpreter, arguments, caller) -> {
      final java.util.Scanner scanner = interpreter.system_in;
      return scanner.hasNextLine() ? scanner.nextLine() : null;
    }));

    result.add(new LoxNative("nextInt", 0, (interpreter, arguments, caller) -> {
      final java.util.Scanner scanner = interpreter.system_in;
      return scanner.hasNextInt() ? Double.valueOf(scanner.nextInt()) : null;
    }));

    result.add(new LoxNative("nextDouble", 0, (interpreter, arguments, caller) -> {
      final java.util.Scanner scanner = interpreter.system_in;
      return scanner.hasNextDouble() ? Double.valueOf(scanner.nextDouble()) : null;
    }));

    result.add(new LoxNative("substr", 3, (interpreter, arguments, caller) -> {
      final Object first = arguments.get(0), second = arguments.get(1), third = arguments.get(2);
      if (!(first instanceof String))
        throw new RuntimeError(caller, "Expected String as first argument to substr, got " + Interpreter.getClassName(first));
      if (!(second instanceof Double))
        throw new RuntimeError(caller, "Expected Double as second argument to substr, got " + Interpreter.getClassName(second));
      if (!(third instanceof Double))
        throw new RuntimeError(caller, "Expected Double as third argument to substr, got " + Interpreter.getClassName(third));
      final String string = (String)arguments.get(0);
      final int start = (int) Math.floor((Double) arguments.get(1));
      final int end = (int) Math.floor((Double) arguments.get(2));
      return string.substring(start, end);
    }));

    result.add(new LoxNative("length", 1, (interpreter, arguments, caller) -> {
      final Object string = arguments.get(0);
      if (!(string instanceof String))
        throw new RuntimeError(caller, "Expected String as first argument to length, got " + Interpreter.getClassName(string));
      return Double.valueOf(((String) string).length());
    }));

    return result;
  }
}
