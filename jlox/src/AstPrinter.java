package com.craftinginterpreters.lox;

import com.craftinginterpreters.lox.Expr;

// Creates an unambiguous, if ugly, string representation of AST nodes.
class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
  String print(final Stmt stmt) { return stmt.accept(this); }
  String print(final Expr expr) { return expr.accept(this); }

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
    return parenthesize("if", expr.condition, expr.true_expr, expr.false_expr);
  }

  @Override
  public String visitVariableExpr(final Expr.Variable expr) {
    return "'" + expr.name.lexeme;
  }

  @Override
  public String visitAssignExpr(final Expr.Assign expr) {
    return parenthesize("assign", new Expr.Variable(expr.name), expr.value);
  }

  @Override
  public String visitLogicalExpr(final Expr.Logical expr) {
    return parenthesize((expr.operator.type == TokenType.OR ? "or" : "and"), expr.left, expr.right);
  }

  @Override
  public String visitVarStmt(final Stmt.Var stmt) {
    return parenthesize("let", new Expr.Variable(stmt.name), stmt.initializer);
  }

  @Override
  public String visitPrintStmt(final Stmt.Print stmt) {
    return parenthesize("print", stmt.expression);
  }

  @Override
  public String visitExpressionStmt(final Stmt.Expression stmt) {
    return stmt.expression.accept(this);
  }

  @Override
  public String visitBlockStmt(final Stmt.Block stmt) {
    return parenthesize("block", stmt.statements.toArray(new Stmt[]{}));
  }

  @Override
  public String visitIfStmt(final Stmt.If stmt) {
    return parenthesize("cond", new Stmt.Expression(stmt.condition), stmt.true_branch, stmt.false_branch);
  }

  @Override
  public String visitWhileStmt(final Stmt.While stmt) {
    return parenthesize("loop", new Stmt.Expression(stmt.condition), stmt.body);
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

  private String parenthesize(String name, Stmt... stmts) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name).append("\n");
    for (final Stmt stmt : stmts) {
      builder.append(stmt.accept(this));
      builder.append("\n");
    }
    builder.append(")");
    return builder.toString();
  }
}
