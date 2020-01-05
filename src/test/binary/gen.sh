#!/bin/bash

SOURCE_FILE=$1
CAHP_RUNTIME="/home/naoki/Sync/mitou2019/kvsp/build/cahp-rt"
LLVM_CC="/home/naoki/Sync/mitou2019/kvsp/build/bin/clang"
CONVERTER="/home/naoki/Sync/mitou2019/cahp-emerald/utils/main"

OBJ_FILE=${SOURCE_FILE%.*}
OBJ_FILE="${OBJ_FILE}.o"

BIN_FILE=${SOURCE_FILE%.*}
BIN_FILE="${BIN_FILE}.bin"

$LLVM_CC -Oz -target cahp $SOURCE_FILE -o $OBJ_FILE --sysroot=$CAHP_RUNTIME -nostdlib
echo $BIN_FILE

$CONVERTER $OBJ_FILE > $BIN_FILE
