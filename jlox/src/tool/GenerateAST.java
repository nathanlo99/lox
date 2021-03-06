package com.craftinginterpreters.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAST {
  public static void main(final String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(1);
    }
    final String outputDir = args[0];
    defineAst(outputDir, "Expr", Arrays.asList(
      "Binary   : Expr left, Token operator, Expr right",
      "Grouping : Expr expression",
      "Literal  : Object value",
      "Unary    : Token operator, Expr right",
      "Comma    : Expr left, Expr right",
      "Ternary  : Expr condition, Expr true_expr, Expr false_expr",
      "Variable : Token name",
      "Assign   : Token name, Expr value",
      "Logical  : Expr left, Token operator, Expr right",
      "Call     : Expr callee, Token paren, List<Expr> arguments",
      "Get      : Expr object, Token name",
      "Set      : Expr object, Token name, Expr value",
      "This     : Token keyword",
      "Super    : Token keyword, Token method"
    ));

    defineAst(outputDir, "Stmt", Arrays.asList(
      "Expression   : Expr expression",
      "Var          : Token name, Expr initializer",
      "Block        : List<Stmt> statements",
      "If           : Expr condition, Stmt true_branch, Stmt false_branch",
      "While        : Expr condition, Stmt body",
      "Function     : Token name, List<Token> params, List<Stmt> body, boolean is_static, boolean is_getter",
      "Return       : Token keyword, Expr value",
      "Break        : Token keyword",
      "Continue     : Token keyword",
      "Class        : Token name, List<Stmt.Function> methods, Expr.Variable superclass"
    ));
  }

  private static void defineAst(final String outputDir, final String baseName, final List<String> types) throws IOException {
    final String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    writer.println("package com.craftinginterpreters.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.println("abstract class " + baseName + " {");

    defineVisitor(writer, baseName, types);

    // The AST classes.
    for (final String type : types) {
      final String className = type.split(":")[0].trim();
      final String fields = type.split(":")[1].trim();
      defineType(writer, baseName, className, fields);
    }

    // The base accept() method.
    writer.println("  abstract <R> R accept(final Visitor<R> visitor);");
    writer.println("}");
    writer.close();
  }

  private static void defineType(PrintWriter writer, final String baseName, final String className, final String fieldList) {
    writer.println("  static class " + className + " extends " + baseName + " {");

    // Constructor.
    writer.print("    " + className + "(");

    // Store parameters in fields.
    final String[] fields = fieldList.split(", ");
    boolean first = true;
    for (final String field : fields) {
      final String[] tokens = field.split(" ");
      final String type = tokens[0], name = tokens[1];
      if (first) { first = false; }
      else { writer.print(", "); }
      writer.print("final " + type + " " + name);
    }
    writer.println(") {");
    for (final String field : fields) {
      final String name = field.split(" ")[1];
      writer.println("      this." + name + " = " + name + ";");
    }
    writer.println("    }");

    // Visitor pattern.
    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(final Visitor<R> visitor) {");
    writer.println("      return visitor.visit" + className + baseName + "(this);");
    writer.println("    }");

    // Fields.
    writer.println();
    for (final String field : fields) {
      writer.println("    final " + field + ";");
    }

    writer.println("  }");
    writer.println();
  }

  private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
    writer.println("  interface Visitor<R> {");

    for (final String type : types) {
      final String typeName = type.split(":")[0].trim();
      writer.println("    R visit" + typeName + baseName + "(final " + typeName + " " + baseName.toLowerCase() + ");");
    }

    writer.println("  }");
    writer.println();
  }

}
