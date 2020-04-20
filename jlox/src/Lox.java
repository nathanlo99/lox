package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
  static boolean hadError = false, hadRuntimeError = false;
  private static final Interpreter interpreter = new Interpreter();

  public static void main(final String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  private static void runFile(final String path) throws IOException {
    final byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()));
    if (hadError) System.exit(65);
    if (hadRuntimeError) System.exit(70);
  }

  private static void runPrompt() throws IOException {
    final InputStreamReader input = new InputStreamReader(System.in);
    final BufferedReader reader = new BufferedReader(input);

    System.out.println("Starting REPL...");
    while (true) {
      System.out.print("> ");
      final String line = reader.readLine();
      if (line == null) break;
      run(line);
      hadError = false;
    }
    System.out.println("Exiting REPL");
  }

  private static void run(final String source) {
    final List<Token> tokens = new Scanner(source).scanTokens();
    final List<Stmt> statements = new Parser(tokens).parseProgram();
    if (hadError) return;
    // for (final Token token : tokens) System.out.println(token);
    for (final Stmt stmt : statements) System.out.println(new AstPrinter().print(stmt));
    interpreter.interpret(statements);
  }

  static void error(final int line, final int column, final String message) {
    report(line, column, "", message);
  }

  static void runtimeError(final RuntimeError error) {
    System.err.println("RuntimeError [" + error.token.line + ":" + error.token.start_column + "] " + error.getMessage());
    hadRuntimeError = true;
  }

  private static void report(final int line, final int column, final String where, final String message) {
    System.err.println("[" + line + ":" + column + "] Error" + where + ": " + message);
    hadError = true;
  }

  static void error(final Token token, final String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, token.start_column, " at end", message);
    } else {
      report(token.line, token.start_column, " at '" + token.lexeme + "'", message);
    }
  }
}
