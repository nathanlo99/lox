
all: src/*.java
	javac -d build/tool src/tool/*.java
	java -cp build/tool com.craftinginterpreters.tool.GenerateAST src
	javac -d build src/*.java
	java -cp build com.craftinginterpreters.lox.Lox $(RUN_ARGS)
