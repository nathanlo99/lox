package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javafx.util.Pair;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  Interpreter() {
    globals.define("clock", true, new LoxNative(0, (interpreter, arguments, caller) -> {
      return (double)System.currentTimeMillis() / 1000.0;
    }));

    globals.define("print", true, new LoxNative(1, (interpreter, arguments, caller) -> {
      System.out.print(stringify(arguments.get(0)));
      return null;
    }));
    globals.define("println", true, new LoxNative(1, (interpreter, arguments, caller) -> {
      System.out.println(stringify(arguments.get(0)));
      return null;
    }));

    globals.define("random", true, new LoxNative(2, (interpreter, arguments, caller) -> {
      final double a = (Double)arguments.get(0), b = (Double)arguments.get(1);
      return Math.random() * (b - a) + a;
    }));

    globals.define("assert", true, new LoxNative(1, (interpreter, arguments, caller) -> {
      if (!isTruthy(arguments.get(0)))
        throw new RuntimeError(caller, "Assertion failed");
      return null;
    }));
  }

  public void interpret(final List<Stmt> statements) {
    try {
      for (final Stmt statement : statements) {
        execute(statement);
      }
    } catch (final RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  public Object evaluate(final Expr expr) {
    return (expr == null) ? null : expr.accept(this);
  }

  private void execute(final Stmt stmt) {
    if (stmt != null) stmt.accept(this);
  }

  public void resolve(final Expr expr, final int depth) {
    locals.put(expr, depth);
  }

  private Object lookup(final Token name, final Expr expr) {
    final Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name);
    } else {
      return globals.get(name);
    }
  }

  private void assign(final Token name, final Expr expr, final Object value) {
    final Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, name, value);
    } else {
      globals.assign(name, value);
    }
  }

  public void executeBlock(final List<Stmt> statements, final Environment environment) {
    final Environment previous = this.environment;
    try {
      this.environment = environment;
      for (final Stmt statement : statements)
        execute(statement);
    } finally {
      this.environment = previous;
    }
  }

  private String stringify(final Object object) {
    if (object == null) return "nil";
    final String result = object.toString();
    if (object instanceof Double && result.endsWith(".0"))
      return result.substring(0, result.length() - 2);
    return result;
  }

  private boolean isTruthy(final Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean) object;
    return true;
  }

  private boolean isEqual(final Object a, final Object b) {
    if (a == null && b == null) return true;
    if (a == null || b == null) return false;
    if (a instanceof Boolean && b instanceof Boolean)
      return (boolean) a == (boolean) b;
    if (a instanceof Double && b instanceof Double)
      return (double) a == (double) b;
    if (a instanceof String && b instanceof String)
      return (String) a == (String) b;
    return false;
  }

  private void assertNumerical(final Token operator, final Object operand) {
    if (!(operand instanceof Double))
      throw new RuntimeError(operator, "Operand must be numerical");
  }

  @Override
  public Object visitLiteralExpr(final Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitGroupingExpr(final Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitUnaryExpr(final Expr.Unary expr) {
    final Object right = evaluate(expr.right);
    switch (expr.operator.type) {
      case MINUS:
        assertNumerical(expr.operator, right);
        return -(double)right;
      case BANG:
        return !isTruthy(right);
    }
    return null;
  }

  @Override
  public Object visitBinaryExpr(final Expr.Binary expr) {
    final Object left = evaluate(expr.left), right = evaluate(expr.right);
    switch (expr.operator.type) {
      case PLUS:
        if (left instanceof String || right instanceof String) {
          return stringify(left) + stringify(right);
        } else if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }
        throw new RuntimeError(expr.operator, "Operands to '+' must be two numbers or strings");
      case MINUS:
        assertNumerical(expr.operator, left); assertNumerical(expr.operator, right);
        return (double) left - (double) right;
      case SLASH:
        assertNumerical(expr.operator, left); assertNumerical(expr.operator, right);
        if ((double) right == 0.0)
          throw new RuntimeError(expr.operator, "Division by zero");
        return (double) left / (double) right;
      case STAR:
        assertNumerical(expr.operator, left); assertNumerical(expr.operator, right);
        return (double) left * (double) right;
      case GREATER:
        assertNumerical(expr.operator, left); assertNumerical(expr.operator, right);
        return (double) left > (double) right;
      case GREATER_EQUAL:
        assertNumerical(expr.operator, left); assertNumerical(expr.operator, right);
        return (double) left >= (double) right;
      case LESS:
        assertNumerical(expr.operator, left); assertNumerical(expr.operator, right);
        return (double) left < (double) right;
      case LESS_EQUAL:
        assertNumerical(expr.operator, left); assertNumerical(expr.operator, right);
        return (double) left <= (double) right;
      case EQUAL_EQUAL:
        return isEqual(left, right);
      case BANG_EQUAL:
        return !isEqual(left, right);
    }
    return null;
  }

  @Override
  public Object visitTernaryExpr(final Expr.Ternary expr) {
    final Object condition = evaluate(expr.condition);
    return isTruthy(condition) ? evaluate(expr.true_expr) : evaluate(expr.false_expr);
  }

  @Override
  public Object visitCommaExpr(final Expr.Comma expr) {
    evaluate(expr.left);
    return evaluate(expr.right);
  }

  @Override
  public Object visitVariableExpr(final Expr.Variable expr) {
    return lookup(expr.name, expr);
  }

  @Override
  public Object visitAssignExpr(final Expr.Assign expr) {
    final Object value = evaluate(expr.value);
    assign(expr.name, expr, value);
    return value;
  }

  @Override
  public Object visitLogicalExpr(final Expr.Logical expr) {
    final Object left = evaluate(expr.left);
    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }
    return evaluate(expr.right);
  }

  @Override
  public Object visitCallExpr(final Expr.Call expr) {
    final Object callee = evaluate(expr.callee);
    final List<Object> arguments = new ArrayList<>();
    for (final Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }
    if (!(callee instanceof LoxCallable))
      throw new RuntimeError(expr.paren, "Expression not callable");
    final LoxCallable function = (LoxCallable) callee;
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
    }
    return function.call(this, arguments, expr.paren);
  }

  @Override
  public Object visitGetExpr(final Expr.Get expr) {
    final Object object = evaluate(expr.object);
    if (!(object instanceof LoxInstance)) {
      throw new RuntimeError(expr.name, "Only instances have properties.");
    }
    return ((LoxInstance) object).get(expr.name);
  }

  @Override
  public Object visitSetExpr(final Expr.Set expr) {
    final Object object = evaluate(expr.object);
    if (!(object instanceof LoxInstance)) {
      throw new RuntimeError(expr.name, "Only instances have fields.");
    }
    final Object value = evaluate(expr.value);
    ((LoxInstance) object).set(expr.name, value);
    return value;
  }

  @Override
  public Object visitThisExpr(final Expr.This expr) {
    return lookup(expr.keyword, expr);
  }

  @Override
  public Object visitSuperExpr(final Expr.Super expr) {
    final int distance = locals.get(expr);
    final LoxClass superclass = (LoxClass)environment.getAt(distance, "super");
    final LoxInstance object = (LoxInstance)environment.getAt(distance - 1, "this");
    if (expr.method.lexeme.equals("__class__")) return superclass;
    final LoxFunction method = superclass.findMethod(expr.method.lexeme);
    if (method == null) {
      throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
    }
    if (method.type == LoxFunctionType.INSTANCE_GETTER || method.type == LoxFunctionType.STATIC_GETTER)
      return method.bind(object).call(this, new ArrayList<Object>(), expr.method);
    return method.bind(object);
  }

  @Override
  public Void visitExpressionStmt(final Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitVarStmt(final Stmt.Var stmt) {
    final Object value = evaluate(stmt.initializer);
    environment.define(stmt.name.lexeme, stmt.initializer != null, value);
    return null;
  }

  @Override
  public Void visitBlockStmt(final Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitIfStmt(final Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.true_branch);
    } else {
      execute(stmt.false_branch);
    }
    return null;
  }

  @Override
  public Void visitWhileStmt(final Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      try {
        execute(stmt.body);
      } catch (final Break break_stmt) {
        break;
      } catch (final Continue continue_stmt) {
        continue;
      }
    }
    return null;
  }

  @Override
  public Void visitFunctionStmt(final Stmt.Function stmt) {
    final LoxFunction function = new LoxFunction(stmt, environment, LoxFunctionType.FUNCTION);
    environment.define(stmt.name.lexeme, true, function);
    return null;
  }

  @Override
  public Void visitReturnStmt(final Stmt.Return stmt) {
    throw new Return(evaluate(stmt.value));
  }

  @Override
  public Void visitBreakStmt(final Stmt.Break stmt) {
    throw new Break();
  }

  @Override
  public Void visitContinueStmt(final Stmt.Continue stmt) {
    throw new Continue();
  }

  @Override
  public Void visitClassStmt(final Stmt.Class stmt) {
    final Object superclass = evaluate(stmt.superclass);
    if (stmt.superclass != null && !(superclass instanceof LoxClass))
      throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");

    environment.define(stmt.name.lexeme, false, null);
    if (stmt.superclass != null) {
      environment = new Environment(environment);
      environment.define("super", true, superclass);
    }
    final Map<String, LoxFunction> methods = new HashMap<>();
    final List<Pair<LoxFunction, Token>> static_blocks = new ArrayList<>();
    for (final Stmt.Function method : stmt.methods) {
      final boolean is_initializer = method.name.lexeme.equals("init");
      final boolean is_static_init = method.name.lexeme.equals("static");
      final boolean is_static_get = method.is_static && method.is_getter;
      final LoxFunctionType type = is_static_init   ? LoxFunctionType.STATIC_INIT :
                                   is_initializer   ? LoxFunctionType.INITIALIZER :
                                   is_static_get    ? LoxFunctionType.STATIC_GETTER :
                                   method.is_static ? LoxFunctionType.STATIC_METHOD :
                                   method.is_getter ? LoxFunctionType.INSTANCE_GETTER :
                                                      LoxFunctionType.INSTANCE_METHOD;
      final LoxFunction function = new LoxFunction(method, environment, type);
      if (is_static_init)
        static_blocks.add(new Pair<>(function, method.name));
      else
        methods.put(method.name.lexeme, function);
    }
    final LoxClass _class = new LoxClass(stmt.name.lexeme, methods, (LoxClass)superclass, this);
    environment.assign(stmt.name, _class);
    for (final Pair<LoxFunction, Token> pair : static_blocks) {
      final LoxFunction static_method = pair.getKey();
      final Token keyword = pair.getValue();
      static_method.bind((LoxInstance)_class).call(this, new ArrayList<>(), keyword);
    }
    if (superclass != null) {
      environment = environment.parent;
    }
    return null;
  }
}
