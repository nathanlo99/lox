#!/bin/bash
for file in tests/*.lox ; do
  echo $file
  make run -s RUN_ARGS=$file > ${file/.lox/.out} ;
  tmp=${file/tests/tests\/correct}
  diff ${file/.lox/.out} ${tmp/.lox/.out} > ${file/.lox/.diff} ;
  error=$?
  if [ $error -eq 0 ]
  then
    rm ${file/.lox/.diff}
    rm ${file/.lox/.out}
  else
    echo "Test $file failed."
  fi
done
