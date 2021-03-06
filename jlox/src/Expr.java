package com.craftinginterpreters.lox;

import java.util.List;

abstract class Expr {
  interface Visitor<R> {
    R visitBinaryExpr(final Binary expr);
    R visitGroupingExpr(final Grouping expr);
    R visitLiteralExpr(final Literal expr);
    R visitUnaryExpr(final Unary expr);
    R visitCommaExpr(final Comma expr);
    R visitTernaryExpr(final Ternary expr);
    R visitVariableExpr(final Variable expr);
    R visitAssignExpr(final Assign expr);
    R visitLogicalExpr(final Logical expr);
    R visitCallExpr(final Call expr);
    R visitGetExpr(final Get expr);
    R visitSetExpr(final Set expr);
    R visitThisExpr(final This expr);
    R visitSuperExpr(final Super expr);
  }

  static class Binary extends Expr {
    Binary(final Expr left, final Token operator, final Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  static class Grouping extends Expr {
    Grouping(final Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;
  }

  static class Literal extends Expr {
    Literal(final Object value) {
      this.value = value;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;
  }

  static class Unary extends Expr {
    Unary(final Token operator, final Expr right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }

  static class Comma extends Expr {
    Comma(final Expr left, final Expr right) {
      this.left = left;
      this.right = right;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitCommaExpr(this);
    }

    final Expr left;
    final Expr right;
  }

  static class Ternary extends Expr {
    Ternary(final Expr condition, final Expr true_expr, final Expr false_expr) {
      this.condition = condition;
      this.true_expr = true_expr;
      this.false_expr = false_expr;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitTernaryExpr(this);
    }

    final Expr condition;
    final Expr true_expr;
    final Expr false_expr;
  }

  static class Variable extends Expr {
    Variable(final Token name) {
      this.name = name;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }

    final Token name;
  }

  static class Assign extends Expr {
    Assign(final Token name, final Expr value) {
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }

    final Token name;
    final Expr value;
  }

  static class Logical extends Expr {
    Logical(final Expr left, final Token operator, final Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitLogicalExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  static class Call extends Expr {
    Call(final Expr callee, final Token paren, final List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }

    final Expr callee;
    final Token paren;
    final List<Expr> arguments;
  }

  static class Get extends Expr {
    Get(final Expr object, final Token name) {
      this.object = object;
      this.name = name;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitGetExpr(this);
    }

    final Expr object;
    final Token name;
  }

  static class Set extends Expr {
    Set(final Expr object, final Token name, final Expr value) {
      this.object = object;
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitSetExpr(this);
    }

    final Expr object;
    final Token name;
    final Expr value;
  }

  static class This extends Expr {
    This(final Token keyword) {
      this.keyword = keyword;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitThisExpr(this);
    }

    final Token keyword;
  }

  static class Super extends Expr {
    Super(final Token keyword, final Token method) {
      this.keyword = keyword;
      this.method = method;
    }

    @Override
    <R> R accept(final Visitor<R> visitor) {
      return visitor.visitSuperExpr(this);
    }

    final Token keyword;
    final Token method;
  }

  abstract <R> R accept(final Visitor<R> visitor);
}
