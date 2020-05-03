
all: src/Expr.java src/Stmt.java src/*.java
	javac -d build src/*.java
	java -cp build com.craftinginterpreters.lox.Lox $(RUN_ARGS)

src/Expr.java src/Stmt.java: src/tool/GenerateAST.java
	javac -d build src/tool/GenerateAST.java
	java -cp build com.craftinginterpreters.tool.GenerateAST src
