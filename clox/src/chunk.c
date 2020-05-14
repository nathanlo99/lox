
#include <stdlib.h>

#include "chunk.h"
#include "memory.h"

void initChunk(Chunk* chunk) {
  chunk->count = 0;
  chunk->capacity = 0;
  chunk->code = NULL;
  chunk->lines = NULL;
  initValueArray(&chunk->constants);
}

void freeChunk(Chunk* chunk) {
  FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
  FREE_ARRAY(int, chunk->lines, chunk->capacity);
  freeValueArray(&chunk->constants);
  initChunk(chunk);
}

void writeChunk(Chunk* chunk, const uint8_t byte, const int line) {
  if (chunk->capacity < chunk->count + 1) {
    const int old_capacity = chunk->capacity;
    chunk->capacity = GROW_CAPACITY(old_capacity);
    chunk->code = GROW_ARRAY(chunk->code, uint8_t, old_capacity, chunk->capacity);
    chunk->lines = GROW_ARRAY(chunk->lines, int, old_capacity, chunk->capacity);
  }
  chunk->code[chunk->count] = byte;
  chunk->lines[chunk->count] = line;
  chunk->count++;
}

int addConstant(Chunk* chunk, const Value value) {
  writeValueArray(&chunk->constants, value);
  return chunk->constants.count - 1;
}

void writeConstant(Chunk* chunk, const Value value, const int line) {
  const int constant = addConstant(chunk, value);
  if (constant <= 255) {
    writeChunk(chunk, OP_CONSTANT, line);
    writeChunk(chunk, constant, line);
  } else {
    writeChunk(chunk, OP_CONSTANT_LONG, line);
    writeChunk(chunk, (constant >> 16) & 0xFF, line);
    writeChunk(chunk, (constant >>  8) & 0xFF, line);
    writeChunk(chunk, (constant >>  0) & 0xFF, line);
  }
}
