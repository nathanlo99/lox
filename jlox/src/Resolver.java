package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private enum FunctionType {
    NONE,
    FUNCTION
  }

  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;

  Resolver(final Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  private void resolve(final Expr expr) {
    if (expr != null) expr.accept(this);
  }

  private void resolve(final Stmt stmt) {
    if (stmt != null) stmt.accept(this);
  }

  void resolve(final List<Stmt> statements) {
    for (final Stmt statement : statements) resolve(statement);
  }

  private void resolveLocal(final Expr expr, final Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
    // Not found. Assume it is global.
  }

  private void resolveFunction(final Stmt.Function function, final FunctionType type) {
    final FunctionType enclosingFunction = currentFunction;
    currentFunction = type;
    beginScope();
    for (final Token param : function.params) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();
    currentFunction = enclosingFunction;
  }

  private void declare(final Token name) {
    if (scopes.isEmpty()) return;
    final Map<String, Boolean> scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name, "Variable with this name already declared in this scope.");
    }
    scope.put(name.lexeme, false);
  }

  private void define(final Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().put(name.lexeme, true);
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }

  private void endScope() {
    scopes.pop();
  }

  @Override
  public Void visitBlockStmt(final Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitVarStmt(final Stmt.Var stmt) {
    declare(stmt.name);
    resolve(stmt.initializer);
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitVariableExpr(final Expr.Variable expr) {
    if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      Lox.error(expr.name, "Cannot read local variable in its own initializer.");
    }
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitAssignExpr(final Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitFunctionStmt(final Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);
    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitExpressionStmt(final Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitIfStmt(final Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.true_branch);
    resolve(stmt.false_branch);
    return null;
  }

  @Override
  public Void visitPrintStmt(final Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitReturnStmt(final Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Cannot return from top-level code.");
    }
    resolve(stmt.value);
    return null;
  }

  @Override
  public Void visitBreakStmt(final Stmt.Break stmt) {
    // TODO: Check loop containment
    return null;
  }

  @Override
  public Void visitContinueStmt(final Stmt.Continue stmt) {
    // TODO: Check loop containment
    return null;
  }

  @Override
  public Void visitWhileStmt(final Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitBinaryExpr(final Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(final Expr.Call expr) {
    resolve(expr.callee);
    for (final Expr argument : expr.arguments) resolve(argument);
    return null;
  }

  @Override
  public Void visitGroupingExpr(final Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(final Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(final Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitUnaryExpr(final Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitTernaryExpr(final Expr.Ternary expr) {
    resolve(expr.condition);
    resolve(expr.true_expr);
    resolve(expr.false_expr);
    return null;
  }

  @Override
  public Void visitCommaExpr(final Expr.Comma expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }
}
