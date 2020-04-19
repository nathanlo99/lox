package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Expr;

// Creates an unambiguous, if ugly, string representation of AST nodes.
class AstPrinter implements Expr.Visitor<String> {
  String print(final Expr expr) {
    return expr.accept(this);
  }

  @Override
  public String visitBinaryExpr(final Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(final Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(final Expr.Literal expr) {
    if (expr.value == null) return "nil";
    if (expr.value instanceof String) return "\"" + expr.value + "\"";
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(final Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitCommaExpr(final Expr.Comma expr) {
    return parenthesize(",", expr.left, expr.right);
  }

  @Override
  public String visitTernaryExpr(final Expr.Ternary expr) {
    return parenthesize("cond", expr.condition, expr.true_expr, expr.false_expr);
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);
    for (final Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }
}
