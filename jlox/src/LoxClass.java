
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

  private boolean hasSuperInit() {
    if (!methods.containsKey("init")) return false;
    final LoxFunction init = methods.get("init");
    if (init.declaration.body.isEmpty()) return false;
    if (!(init.declaration.body.get(0) instanceof Stmt.Expression)) return false;
    final Stmt.Expression expr_stmt = (Stmt.Expression)init.declaration.body.get(0);
    if (!(expr_stmt.expression instanceof Expr.Call)) return false;
    final Expr.Call call_expr = (Expr.Call)expr_stmt.expression;
    if (!(call_expr.callee instanceof Expr.Super)) return false;
    final Expr.Super super_expr = (Expr.Super)call_expr.callee;
    if (!super_expr.method.lexeme.equals("init")) return false;
    return true;
  }

  @Override
  public Object call(final Interpreter interpreter, final List<Object> arguments, final Token caller) {
    final LoxInstance instance = new LoxInstance(this, interpreter);
    final LoxFunction initializer = findMethod("init");
    final boolean super_init_called = hasSuperInit();
    if (superclass != null && !super_init_called)
      superclass.call(interpreter, arguments, caller);
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
