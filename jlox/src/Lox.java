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
  static boolean allowFieldCreation = true;
  private static final Interpreter interpreter = new Interpreter();
  private static String source[];

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
    Lox.source = source.split("\\r?\\n");
    final List<Token> tokens = new Scanner(source).scanTokens();
    final List<Stmt> statements = new Parser(tokens).parseProgram();
    if (hadError) return;
    // for (final Token token : tokens) System.out.println(token);
    // for (final Stmt stmt : statements) System.out.println(new AstPrinter().print(stmt));
    new Resolver(interpreter).resolve(statements);
    if (hadError) return;
    interpreter.interpret(statements);
  }

  static void error(final int line, final int column, final String message) {
    report("Error", line, column, message);
  }

  static void runtimeError(final RuntimeError error) {
    report("RuntimeError", error.token.line, error.token.start_column, error.getMessage());
    hadRuntimeError = true;
  }

  private static void report(final String type, final int line, final int column, final String message) {
    System.err.println(type + ": " + message + " [" + line + ":" + column + "] ");
    System.err.println(Lox.source[line - 1]);
    System.err.println(new String(new char[column - 1]).replace("\0", " ") + "^");
  }

  static void warning(final Token token, final String message) {
    report("Warning", token.line, token.start_column, message);
  }

  static void error(final Token token, final String message) {
    report("Error", token.line, token.start_column, message);
    hadError = true;
  }
}
