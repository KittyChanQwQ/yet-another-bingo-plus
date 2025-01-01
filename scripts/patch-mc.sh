#!/bin/bash
set -e

# Usage: bash scripts/patch-mc.sh mc1.21.3/src/main/java/me/jfenn/bingo/impl/EntityManagerImpl.kt
# $1 = input file

TMPDIR=/tmp/patch-mc.$$

mkdir -p $TMPDIR

git diff -p $1 > $TMPDIR/file.patch

for impl in ./mc*; do
  implFile="$impl/${1#*/}"
  echo "$implFile"
  patch -f $implFile $TMPDIR/file.patch || true
done

rm -rf $TMPDIR
