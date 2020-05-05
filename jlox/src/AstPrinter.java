package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import com.craftinginterpreters.lox.Expr;

// Creates an unambiguous, if ugly, string representation of AST nodes.
class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
  private String block_indent = "";

  String print(final Stmt stmt) { return block_indent + (stmt == null ? "" : stmt.accept(this)); }
  String print(final Expr expr) { return expr == null ? "" : expr.accept(this); }

  @Override
  public String visitBinaryExpr(final Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(final Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  private String escapeString(final String str) {
    return str.replace("\n", "\\n").replace("\t", "\\t");
  }

  @Override
  public String visitLiteralExpr(final Expr.Literal expr) {
    if (expr.value == null) return "nil";
    if (expr.value instanceof String) return "\"" + escapeString((String)expr.value) + "\"";
    final String result = expr.value.toString();
    if (expr.value instanceof Double && result.endsWith(".0"))
      return result.substring(0, result.length() - 2);
    return result;
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
  public String visitCallExpr(final Expr.Call expr) {
    return parenthesize(print(expr.callee), expr.arguments.toArray(new Expr[]{}));
  }

  @Override
  public String visitGetExpr(final Expr.Get expr) {
    return parenthesize("get", expr.object, new Expr.Variable(expr.name));
  }

  @Override
  public String visitSetExpr(final Expr.Set expr) {
    return parenthesize("set", expr.object, new Expr.Variable(expr.name), expr.value);
  }

  @Override
  public String visitThisExpr(final Expr.This expr) {
    return visitVariableExpr(new Expr.Variable(expr.keyword));
  }

  @Override
  public String visitVarStmt(final Stmt.Var stmt) {
    return parenthesize("var", new Expr.Variable(stmt.name), stmt.initializer);
  }

  @Override
  public String visitExpressionStmt(final Stmt.Expression stmt) {
    return stmt.expression == null ? "" : stmt.expression.accept(this);
  }

  @Override
  public String visitBlockStmt(final Stmt.Block stmt) {
    return parenthesize("block", stmt.statements.toArray(new Stmt[]{}));
  }

  @Override
  public String visitIfStmt(final Stmt.If stmt) {
    if (stmt.false_branch == null)
      return parenthesize("cond", new Stmt.Expression(stmt.condition), stmt.true_branch);
    else
      return parenthesize("cond", new Stmt.Expression(stmt.condition), stmt.true_branch, stmt.false_branch);
  }

  @Override
  public String visitWhileStmt(final Stmt.While stmt) {
    return parenthesize("while", new Stmt.Expression(stmt.condition), stmt.body);
  }

  @Override
  public String visitBreakStmt(final Stmt.Break stmt) {
    return "(break)";
  }

  @Override
  public String visitContinueStmt(final Stmt.Continue stmt) {
    return "(continue)";
  }

  @Override
  public String visitFunctionStmt(final Stmt.Function stmt) {
    final List<Expr> params = new ArrayList<>();
    for (final Token token : stmt.params)
      params.add(new Expr.Variable(token));
    final String signature = parenthesize(stmt.name.lexeme, params.toArray(new Expr[]{}));
    return parenthesize("define " + signature, stmt.body.toArray(new Stmt[]{}));
  }

  @Override
  public String visitReturnStmt(final Stmt.Return stmt) {
    return parenthesize("return", stmt.value);
  }

  @Override
  public String visitClassStmt(final Stmt.Class stmt) {
    return "(class " + stmt.name.lexeme + ")";
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);
    for (final Expr expr : exprs) {
      if (expr == null) continue;
      builder.append(" ").append(expr.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }

  private String parenthesize(String name, Stmt... stmts) {
    final String old_indent = block_indent;
    block_indent = old_indent + "  ";
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name).append("\n");
    for (final Stmt stmt : stmts) {
      if (stmt == null) continue;
      builder.append(print(stmt)).append("\n");
    }
    block_indent = old_indent;
    builder.append(old_indent).append(")");
    return builder.toString();
  }
}
