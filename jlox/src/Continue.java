package com.craftinginterpreters.lox;

class Continue extends RuntimeException {
  Continue() {
    super(null, null, false, false); // Disable some JVM exception stuff
  }
}
