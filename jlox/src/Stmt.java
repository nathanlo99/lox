package com.craftinginterpreters.lox;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitExpressionStmt(final Expression stmt);
    R visitVarStmt(final Var stmt);
    R visitBlockStmt(final Block stmt);
    R visitIfStmt(final If stmt);
    R visitWhileStmt(final While stmt);
    R visitFunctionStmt(final Function stmt);
    R visitReturnStmt(final Return stmt);
    R visitBreakStmt(final Break stmt);
    R visitContinueStmt(final Continue stmt);
    R visitClassStmt(final Class stmt);
  }

  static class Expression extends Stmt {
    Expression(final Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }

  static class Var extends Stmt {
    Var(final Token name, final Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

    final Token name;
    final Expr initializer;
  }

  static class Block extends Stmt {
    Block(final List<Stmt> statements) {
      this.statements = statements;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }

    final List<Stmt> statements;
  }

  static class If extends Stmt {
    If(final Expr condition, final Stmt true_branch, final Stmt false_branch) {
      this.condition = condition;
      this.true_branch = true_branch;
      this.false_branch = false_branch;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

    final Expr condition;
    final Stmt true_branch;
    final Stmt false_branch;
  }

  static class While extends Stmt {
    While(final Expr condition, final Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;
    final Stmt body;
  }

  static class Function extends Stmt {
    Function(final Token name, final List<Token> params, final List<Stmt> body, final boolean is_static, final boolean is_getter) {
      this.name = name;
      this.params = params;
      this.body = body;
      this.is_static = is_static;
      this.is_getter = is_getter;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
    }

    final Token name;
    final List<Token> params;
    final List<Stmt> body;
    final boolean is_static;
    final boolean is_getter;
  }

  static class Return extends Stmt {
    Return(final Token keyword, final Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }

    final Token keyword;
    final Expr value;
  }

  static class Break extends Stmt {
    Break(final Token keyword) {
      this.keyword = keyword;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitBreakStmt(this);
    }

    final Token keyword;
  }

  static class Continue extends Stmt {
    Continue(final Token keyword) {
      this.keyword = keyword;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitContinueStmt(this);
    }

    final Token keyword;
  }

  static class Class extends Stmt {
    Class(final Token name, final List<Stmt.Function> methods, final Expr.Variable superclass) {
      this.name = name;
      this.methods = methods;
      this.superclass = superclass;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitClassStmt(this);
    }

    final Token name;
    final List<Stmt.Function> methods;
    final Expr.Variable superclass;
  }

  abstract <R> R accept(final Visitor<R> visitor);
}
