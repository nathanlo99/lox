
package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
  final String name;
  final Map<String, LoxFunction> methods;
  final LoxClass superclass;

  LoxClass(final String name, final Map<String, LoxFunction> methods, final LoxClass superclass, final Interpreter interpreter) {
    super(null, interpreter); this._class = this;
    this.name = name;
    this.methods = methods;
    this.superclass = superclass;
  }

  LoxFunction findMethod(final String name) {
    if (methods.containsKey(name))
      return methods.get(name);
    if (superclass != null)
      return superclass.findMethod(name);
    return null;
  }

  @Override
  public Object call(final Interpreter interpreter, final List<Object> arguments, final Token caller) {
    final LoxInstance instance = new LoxInstance(this, interpreter);
    final LoxFunction initializer = findMethod("init");
    if (initializer != null)
      initializer.bind(instance).call(interpreter, arguments, caller);
    return instance;
  }

  @Override
  public int arity() {
    final LoxFunction initializer = findMethod("init");
    if (initializer == null) return 0;
    return initializer.arity();
  }

  @Override
  public String toString() {
    return "<user-class " + name + ">";
  }
}
