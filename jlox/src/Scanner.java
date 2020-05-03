package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start_idx = 0;
  private int cur_idx = 0;
  private int line = 1;
  private int start_column = 1;
  private int cur_column = 1;

  private static final Map<String, TokenType> keywords;
  static {
    keywords = new HashMap<>();
    keywords.put("and",      AND);
    keywords.put("break",    BREAK);
    keywords.put("class",    CLASS);
    keywords.put("continue", CONTINUE);
    keywords.put("else",     ELSE);
    keywords.put("false",    FALSE);
    keywords.put("for",      FOR);
    keywords.put("fun",      FUN);
    keywords.put("if",       IF);
    keywords.put("nil",      NIL);
    keywords.put("or",       OR);
    keywords.put("print",    PRINT);
    keywords.put("return",   RETURN);
    keywords.put("super",    SUPER);
    keywords.put("this",     THIS);
    keywords.put("true",     TRUE);
    keywords.put("var",      VAR);
    keywords.put("while",    WHILE);
  }

  Scanner(String source) {
    this.source = source;
  }

  private boolean isAtEnd() { return cur_idx >= source.length(); }
  private void advance() {
    if (isAtEnd()) return;
    final char current_char = source.charAt(cur_idx);
    cur_idx++; cur_column++;
    if (current_char == '\n') {
      line++; cur_column = 1;
    }
  }

  private char peek(int offset) {
    return (cur_idx + offset >= source.length()) ? '\0' : source.charAt(cur_idx + offset);
  }
  private char peek() { return peek(0); }

  private boolean match(final char expected) {
    final boolean match = peek() == expected;
    if (match) advance();
    return match;
  }

  private boolean isAlpha(final char c) {
    return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_';
  }
  private boolean isDigit(final char c) { return '0' <= c && c <= '9'; }
  private boolean isAlphanumeric(final char c) { return isAlpha(c) || isDigit(c); }
  private boolean isWhitespace(final char c) { return c == ' ' || c == '\r' || c == '\t' || c == '\n'; }
  private void addToken(final TokenType type) { addToken(type, null); }
  private void addToken(final TokenType type, final Object literal) {
    final String text = source.substring(start_idx, cur_idx);
    tokens.add(new Token(type, text, literal, line, start_column, cur_column));
  }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
      start_idx = cur_idx;
      start_column = cur_column;
      scanToken();
    }
    tokens.add(new Token(EOF, "", null, line, cur_column, cur_column));
    return tokens;
  }

  private void scanToken() {
    final int old_line = line, old_column = cur_column;
    final char c = peek(); advance();
    // System.out.println("Scanning token '" + c + "' at " + old_line + ":" + old_column);
    if (isDigit(c)) {
      scanNumber();
      return;
    } else if (isAlpha(c)) {
      scanIdentifier();
      return;
    } else if (isWhitespace(c)) {
      return;
    }
    switch (c) {
      // Single-character tokens
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA); break;
      case '.': addToken(DOT); break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break;
      case '?': addToken(QUESTION_MARK); break;
      case ':': addToken(COLON); break;

      // One or two character tokens
      case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
      case '=': addToken(match('=') ? EQUAL_EQUAL: EQUAL); break;
      case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
      case '>': addToken(match('=') ? GREATER_EQUAL: GREATER); break;

      // Comments or just a slash?
      case '/':
        if (match('/')) {
          // Line comment
          while (peek() != '\n' && !isAtEnd()) advance();
        } else if (match('*')) {
          // Multi-line comment
          final int start_line = line;
          int depth = 1;
          while (!isAtEnd() && depth > 0) {
            final char cur_char = peek(), next_char = peek(1);
            advance();
            if (cur_char == '*' && next_char == '/') {
              depth--; advance();
            } else if (cur_char == '/' && next_char == '*') {
              depth++; advance();
            }
          }
          if (depth > 0)
            Lox.error(line, cur_column, "Unterminated multi-line comment started at " + start_line + ":" + start_column);
        } else {
          // Boring ol' slash
          addToken(SLASH);
        }
        break;

      // Whitespace
      case ' ': case '\r': case '\t': case '\n':
        break;

      // Strings
      case '"':
        scanString(); break;

      default:
        Lox.error(old_line, old_column, "Unexpected character: '" + c + "'");
        break;
    }
  }

  private void scanString() {
    final int start_line = line, start_column = cur_column - 1;
    // Some extra logic added here for escaped quotes
    char last = '\0', cur;
    while (!isAtEnd() && ((cur = peek()) != '"' || last == '\\')) { last = cur; advance(); }
    if (isAtEnd()) {
      Lox.error(line, cur_column, "Unterminated string (started at " + start_line + ":" + start_column + ")");
      return;
    }
    // Closing ".
    advance();
    // Trim the surrounding quotes and un-escape things
    final String value = source.substring(start_idx + 1, cur_idx - 1)
                               .replace("\\\"", "\"")
                               .replace("\\n", "\n")
                               .replace("\\r", "\r")
                               .replace("\\t", "\t")
                               .replace("\\'", "'");
    addToken(STRING, value);
  }

  private void scanNumber() {
    while (isDigit(peek())) advance();
    // Fractional part?
    if (peek() == '.' && isDigit(peek(1))) {
      advance();
      while (isDigit(peek())) advance();
    }
    addToken(NUMBER, Double.parseDouble(source.substring(start_idx, cur_idx)));
  }

  private void scanIdentifier() {
    while (isAlphanumeric(peek())) advance();
    final String text = source.substring(start_idx, cur_idx);
    final TokenType type = keywords.getOrDefault(text, IDENTIFIER);
    addToken(type);
  }

}
