package com.craftinginterpreters.lox;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import static com.craftinginterpreters.lox.TokenType.*;

/*
program        → declaration* EOF ;
declaration    → functionDecl
                | variableDecl
                | statement ;
functionDecl   → "fun" function ;
function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
variableDecl   → "var" IDENTIFIER ( "=" expression )? ";" ;
statement      → returnStmt
                | exprStmt
                | printStmt
                | ifStmt
                | whileStmt
                | forStmt
                | block ;
returnStmt     → "return" expression? ";" ;
forStmt        → "for" "(" ( variableDecl | exprStmt | ";")
                            expression? ";"
                            expression? ")" statement ;
whileStmt      → "while" "(" expression ")" statement ;
ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
block          → "{" declaration* "}" ;
exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
expression     → assignment ( "," assignment )* ;
assignment     → IDENTIFIER "=" assignment
                | ternary ;
ternary        → logic_or ("?" expression ":" ternary)?  ;
logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
equality       → comparison? ( ( "!=" | "==" ) comparison )* ;
comparison     → addition? ( ( ">" | ">=" | "<" | "<=" ) addition )* ;
sum            → product? ( ( "-" | "+" ) product )* ;
product        → unary? ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
                | call ;
call           → primary ( "(" arguments? ")" )* ;
arguments      → expression ( "," expression )* ;
primary        → NUMBER | STRING
                | "false" | "true" | "nil"
                | "(" _expression_ ")" ;
                | IDENTIFIER;
*/

class Parser {
  private final List<Token> tokens;
  private int cur_idx = 0;

  private static class ParseError extends RuntimeException {}

  Parser(final List<Token> tokens) {
    this.tokens = tokens;
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

  List<Stmt> parseProgram() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(parseDeclaration());
    }
    return statements;
  }

  private Stmt parseDeclaration() {
    try {
      if (match(VAR)) return parseVarDeclaration();
      if (match(FUN)) return parseFunction("function");
      return parseStatement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt.Function parseFunction(final String kind) {
    final Token name = consume(IDENTIFIER, "Expected " + kind + " name.");
    consume(LEFT_PAREN, "Expected '(' after " + kind + " name .");
    final List<Token> parameters = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Cannot have more than 255 parameters.");
        }
        parameters.add(consume(IDENTIFIER, "Expected parameter name."));
      } while (match(COMMA));
    }
    consume(RIGHT_PAREN, "Expected ')' after paramters.");
    consume(LEFT_BRACE, "Expected '{' before " + kind + " body.");
    final List<Stmt> body = parseBlock();
    return new Stmt.Function(name, parameters, body);
  }

  private Stmt parseVarDeclaration() {
    final Token name = consume(IDENTIFIER, "Expected variable name.");
    final Expr initializer = match(EQUAL) ? parseExpression() : null;
    consume(SEMICOLON, "Expected ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  private Stmt parseStatement() {
    if (match(PRINT)) return parsePrintStatement();
    if (match(LEFT_BRACE)) return new Stmt.Block(parseBlock());
    if (match(IF)) return parseIfStatement();
    if (match(WHILE)) return parseWhileStatement();
    if (match(FOR)) return parseForStatement();
    if (match(RETURN)) return parseReturnStatement();
    return parseExpressionStatement();
  }

  private Stmt parseReturnStatement() {
    final Token keyword = previous();
    final Expr value = check(SEMICOLON) ? null : parseExpression();
    consume(SEMICOLON, "Expected ';' after return value.");
    return new Stmt.Return(keyword, value);
  }

  private Stmt parseForStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'for'.");
    final Stmt init = match(SEMICOLON) ? null : (match(VAR) ? parseVarDeclaration() : parseExpressionStatement());
    final Expr condition = check(SEMICOLON) ?  new Expr.Literal(true) : parseExpression();
    consume(SEMICOLON, "Expect ';' after loop condition.");
    final Expr increment = check(RIGHT_PAREN) ? null : parseExpression();
    consume(RIGHT_PAREN, "Expect ')' after for clauses.");
    final Stmt body = parseStatement();

    // De-sugar (NOTE: null represents a no-op)
    return new Stmt.Block(Arrays.asList(
      init,
      new Stmt.While(condition,
        new Stmt.Block(Arrays.asList(
          body,
          new Stmt.Expression(increment)
        )))
    ));
  }

  private Stmt parseWhileStatement() {
    consume(LEFT_PAREN, "Expected '(' after 'while'.");
    final Expr condition = parseExpression();
    consume(RIGHT_PAREN, "Expected ')' after while condition.");
    final Stmt body = parseStatement();
    return new Stmt.While(condition, body);
  }

  private Stmt parseIfStatement() {
    consume(LEFT_PAREN, "Expected '(' after 'if'.");
    final Expr condition = parseExpression();
    consume(RIGHT_PAREN, "Expected ')' after if condition.");
    final Stmt true_branch = parseStatement();
    final Stmt false_branch = match(ELSE) ? parseStatement() : null;
    return new Stmt.If(condition, true_branch, false_branch);
  }

  private List<Stmt> parseBlock() {
    final List<Stmt> statements = new ArrayList<>();
    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(parseDeclaration());
    }
    consume(RIGHT_BRACE, "Expected '}' to end block.");
    return statements;
  }

  private Stmt parseExpressionStatement() {
    final Expr value = parseExpression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Expression(value);
  }

  private Stmt parsePrintStatement() {
    final Expr value = parseExpression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  private Expr parseExpression() {
    Expr result = parseAssignment();
    while (match(COMMA)) {
      final Expr right = parseAssignment();
      result = new Expr.Comma(result, right);
    }
    return result;
  }

  private Expr parseAssignment() {
    final Expr expr = parseLogicalOr();
    if (match(EQUAL)) {
      final Token equals = previous();
      final Expr value = parseAssignment();
      if (!(expr instanceof Expr.Variable))
        error(equals, "Invalid assignment target.");
      final Token name = ((Expr.Variable) expr).name;
      return new Expr.Assign(name, value);
    }
    return expr;
  }

  private Expr parseLogicalOr() {
    Expr expr = parseLogicalAnd();
    while (match(OR)) {
      final Token operator = previous();
      final Expr right = parseLogicalAnd();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
  }

  private Expr parseLogicalAnd() {
    Expr expr = parseTernary();
    while (match(OR)) {
      final Token operator = previous();
      final Expr right = parseTernary();
      expr = new Expr.Logical(expr, operator, right);
    }
    return expr;
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
    return parseCall();
  }

  private Expr parseCall() {
    // primary ( "(" arguments? ")" )*
    Expr expr = parsePrimary();
    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else {
        break;
      }
    }
    return expr;
  }

  private Expr finishCall(final Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255)
          error(peek(), "Cannot have more than 255 arguments.");
        arguments.add(parseAssignment());
      } while (match(COMMA));
    }
    final Token paren = consume(RIGHT_PAREN, "Expected ')' after arguments.");
    return new Expr.Call(callee, paren, arguments);
  }

  private Expr parsePrimary() {
    if (match(FALSE)) { return new Expr.Literal(false); }
    if (match(TRUE)) { return new Expr.Literal(true); }
    if (match(NIL)) { return new Expr.Literal(null); }
    if (match(NUMBER, STRING)) { return new Expr.Literal(previous().literal); }
    if (match(IDENTIFIER)) { return new Expr.Variable(previous()); }
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

  private void synchronize() {
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
