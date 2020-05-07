package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private enum FunctionType {
    NONE,
    FUNCTION,
    METHOD,
    INITIALIZER
  }
  private FunctionType current_function = FunctionType.NONE;

  private enum ClassType {
    NONE,
    CLASS,
    SUBCLASS
  }
  private ClassType current_class = ClassType.NONE;

  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();

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
    final FunctionType enclosingFunction = current_function;
    current_function = type;
    beginScope();
    for (final Token param : function.params) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();
    current_function = enclosingFunction;
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
  public Void visitThisExpr(final Expr.This expr) {
    if (current_class == ClassType.NONE) {
      Lox.error(expr.keyword, "Cannot use 'this' outside of a class.");
      return null;
    }
    resolveLocal(expr, expr.keyword);
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
  public Void visitReturnStmt(final Stmt.Return stmt) {
    if (current_function == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Cannot return from top-level code.");
    }
    if (current_function == FunctionType.INITIALIZER && stmt.value != null) {
      Lox.error(stmt.keyword, "Cannot return value from initializer.");
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
  public Void visitClassStmt(final Stmt.Class stmt) {
    final ClassType parent_class = current_class;
    current_class = stmt.superclass == null ? ClassType.CLASS : ClassType.SUBCLASS;
    declare(stmt.name);
    define(stmt.name);
    if (stmt.superclass != null && stmt.name.lexeme.equals(stmt.superclass.name.lexeme))
      Lox.error(stmt.superclass.name, "Cannot inherit from self.");
    resolve(stmt.superclass);
    if (stmt.superclass != null) {
      beginScope();
      scopes.peek().put("super", true);
    }
    beginScope();
    scopes.peek().put("this", true);
    for (final Stmt.Function method : stmt.methods) {
      final boolean is_initializer = method.name.lexeme.equals("init");
      final FunctionType declaration = is_initializer ? FunctionType.INITIALIZER : FunctionType.METHOD;
      if (is_initializer && method.is_static)
        Lox.error(method.name, "Constructor cannot be static.");
      resolveFunction(method, declaration);
    }
    endScope();
    if (stmt.superclass != null) {
      endScope();
    }
    current_class = parent_class;
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

  @Override
  public Void visitGetExpr(final Expr.Get expr) {
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitSetExpr(final Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitSuperExpr(final Expr.Super expr) {
    if (current_class == ClassType.NONE) {
      Lox.error(expr.keyword, "Cannot use 'super' outside of a class.");
    } else if (current_class != ClassType.SUBCLASS) {
      Lox.error(expr.keyword, "Cannot use 'super' in a class with no superclass.");
    }
    resolveLocal(expr, expr.keyword);
    return null;
  }
}
