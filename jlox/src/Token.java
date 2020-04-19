package com.craftinginterpreters.lox;

class Token {
  final TokenType type;
  final String lexeme;
  final Object literal;
  final int line;
  final int start_column, end_column;

  Token(TokenType type, String lexeme, Object literal, int line, int start_column, int end_column) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
    this.start_column = start_column;
    this.end_column = end_column;
  }

  public String toString() {
    return "Token(type: " + type + ", lexeme: '" + lexeme + "', literal: " + literal + ", loc: " + line + ":" + start_column + "-" + end_column + ")";
  }
}
