
package com.craftinginterpreters.lox;

class Return extends RuntimeException {
  final Object value;

  Return(final Object value) {
    super(null, null, false, false); // Disable some JVM exception stuff
    this.value = value;
  }
}
