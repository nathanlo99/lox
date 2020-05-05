#!/bin/bash
for file in tests/*.lox ; do
  echo $file
  make RUN_ARGS=$file > ${file/.lox/.out} ;
  tmp=${file/tests/tests\/correct}
  diff ${file/.lox/.out} ${tmp/.lox/.out} > ${file/.lox/.diff} ;
  error=$?
  if [ $error -ne 0 ]
  then
    echo "Test $file failed."
  else
    rm ${file/.lox/.diff}
  fi
  rm ${file/.lox/.out}
done
