
tests := tests/*.lox
correct := $(tests:tests/%.lox=tests/correct/%.out)

all: src/Expr.java src/Stmt.java src/*.java
	javac -d build src/*.java

src/Expr.java src/Stmt.java: src/tool/GenerateAST.java
	javac -d build src/tool/GenerateAST.java
	java -cp build com.craftinginterpreters.tool.GenerateAST src

runtests: $(tests) $(correct)
	./runtests.sh
