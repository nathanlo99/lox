package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  final Environment globals = new Environment();
  private Environment environment = globals;

  Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() { return 0; }
      @Override
      public String toString() { return "<native fn>"; }
      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }
    });
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
    return environment.get(expr.name);
  }

  @Override
  public Object visitAssignExpr(final Expr.Assign expr) {
    final Object value = evaluate(expr.value);
    environment.assign(expr.name, value);
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
    return function.call(this, arguments);
  }

  @Override
  public Void visitExpressionStmt(final Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(final Stmt.Print stmt) {
    final Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitVarStmt(final Stmt.Var stmt) {
    final Object value = evaluate(stmt.initializer);
    environment.define(stmt.name.lexeme, value);
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
      execute(stmt.body);
    }
    return null;
  }

  @Override
  public Void visitFunctionStmt(final Stmt.Function stmt) {
    final LoxFunction function = new LoxFunction(stmt, environment);
    environment.define(stmt.name.lexeme, function);
    return null;
  }

  @Override
  public Void visitReturnStmt(final Stmt.Return stmt) {
    throw new Return(evaluate(stmt.value));
  }
}
