#!/bin/bash
for file in tests/*.lox ; do
  echo $file
  if [ -f ${file/.lox/.in} ]; then
    make run -s RUN_ARGS=$file > ${file/.lox/.out} < ${file/.lox/.in} ;
  else
    make run -s RUN_ARGS=$file > ${file/.lox/.out} ;
  fi

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
