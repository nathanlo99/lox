package com.craftinginterpreters.lox;

class Interpreter implements Expr.Visitor<Object> {
  public void interpret(final Expr expression) {
    try {
      final Object value = evaluate(expression);
      System.out.println(stringify(value));
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  public Object evaluate(final Expr expr) {
    return expr.accept(this);
  }

  private String stringify(Object object) {
    if (object == null) return "nil";

    // Hack. Work around Java adding ".0" to integer-valued doubles.
    if (object instanceof Double) {
      final String text = object.toString();
      if (text.endsWith(".0"))
        return text.substring(0, text.length() - 2);
      else
        return text;
    }
    return object.toString();
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
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
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
  public Object visitBinaryExpr(Expr.Binary expr) {
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
  public Object visitTernaryExpr(Expr.Ternary expr) {
    final Object condition = evaluate(expr.condition);
    if (isTruthy(condition)) {
      return evaluate(expr.true_expr);
    } else {
      return evaluate(expr.false_expr);
    }
  }

  @Override
  public Object visitCommaExpr(Expr.Comma expr) {
    evaluate(expr.left);
    return evaluate(expr.right);
  }
}
