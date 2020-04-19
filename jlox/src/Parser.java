package com.craftinginterpreters.lox;

import java.util.List;
import static com.craftinginterpreters.lox.TokenType.*;

/*
expression     → ternary ( "," ternary )* ;
ternary        → equality "?" expression ":" ternary
                | equality ;
equality       → comparison? ( ( "!=" | "==" ) comparison )* ;
comparison     → addition? ( ( ">" | ">=" | "<" | "<=" ) addition )* ;
sum            → product? ( ( "-" | "+" ) product )* ;
product        → unary? ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;
primary        → NUMBER | STRING | "false" | "true" | "nil"
               | "(" expression ")" ;
*/

class Parser {
  private final List<Token> tokens;
  private int cur_idx = 0;

  private static class ParseError extends RuntimeException {}

  Parser(final List<Token> tokens) {
    this.tokens = tokens;
  }

  Expr parse() {
    try {
      return parseExpression();
    } catch (final ParseError error) {
      return null;
    }
  }

  private Token peek() { return tokens.get(cur_idx); }
  private Token previous() { return tokens.get(cur_idx - 1); }
  private boolean isAtEnd() { return peek().type == EOF; }
  private void advance() { if (!isAtEnd()) cur_idx++; }
  private boolean check(final TokenType type) {
    return !isAtEnd() && peek().type == type;
  }

  private boolean match(final TokenType... types) {
    for (final TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private Token consume(final TokenType type, final String message) {
    if (check(type)) { advance(); return previous(); }
    throw error(peek(), message);
  }

  private Expr parseExpression() {
    Expr result = parseTernary();
    while (match(COMMA)) {
      final Expr right = parseTernary();
      result = new Expr.Comma(result, right);
    }
    return result;
  }

  private Expr parseTernary() {
    final Expr condition = parseEquality();
    if (match(QUESTION_MARK)) {
      final Expr true_expr = parseExpression();
      consume(COLON, "Expected ':' to complete ternary expression");
      final Expr false_expr = parseTernary();
      return new Expr.Ternary(condition, true_expr, false_expr);
    } else {
      return condition;
    }
  }

  private Expr parseEquality() {
    if (match(BANG_EQUAL, EQUAL_EQUAL)) {
      final Token operator = previous();
      final Expr right = parseComparison();
      throw error(operator, "Binary expression missing left side");
    }
    Expr result = parseComparison();
    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      final Token operator = previous();
      final Expr right = parseComparison();
      result = new Expr.Binary(result, operator, right);
    }
    return result;
  }

  private Expr parseComparison() {
    if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      final Token operator = previous();
      final Expr right = parseSum();
      throw error(operator, "Binary expression missing left side");
    }
    Expr result = parseSum();
    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      final Token operator = previous();
      final Expr right = parseSum();
      result = new Expr.Binary(result, operator, right);
    }
    return result;
  }

  private Expr parseSum() {
    if (match(PLUS)) {
      final Token operator = previous();
      final Expr right = parseProduct();
      throw error(operator, "Binary expression missing left side");
    }
    Expr result = parseProduct();
    while (match(MINUS, PLUS)) {
      final Token operator = previous();
      final Expr right = parseProduct();
      result = new Expr.Binary(result, operator, right);
    }
    return result;
  }

  private Expr parseProduct() {
    if (match(SLASH, STAR)) {
      final Token operator = previous();
      final Expr right = parseUnary();
      throw error(operator, "Binary expression missing left side");
    }
    Expr result = parseUnary();
    while (match(SLASH, STAR)) {
      final Token operator = previous();
      final Expr right = parseUnary();
      result = new Expr.Binary(result, operator, right);
    }
    return result;
  }

  private Expr parseUnary() {
    if (match(BANG, MINUS)) {
      final Token operator = previous();
      final Expr right = parseUnary();
      return new Expr.Unary(operator, right);
    }
    return parsePrimary();
  }

  private Expr parsePrimary() {
    if (match(FALSE)) { return new Expr.Literal(false); }
    if (match(TRUE)) { return new Expr.Literal(true); }
    if (match(NIL)) { return new Expr.Literal(null); }
    if (match(NUMBER, STRING)) { return new Expr.Literal(previous().literal); }
    if (match(LEFT_PAREN)) {
      final Expr inner = parseExpression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(inner);
    }
    throw error(peek(), "Expected expression");
  }

  private ParseError error(final Token token, final String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private void synch() {
    advance();
    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
}
