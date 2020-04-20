
all: src/*.java
	javac -d build src/tool/*.java
	java -cp build com.craftinginterpreters.tool.GenerateAST src
	javac -d build src/*.java
	java -cp build com.craftinginterpreters.lox.Lox $(RUN_ARGS)
