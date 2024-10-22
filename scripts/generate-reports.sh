#!/bin/bash

SCRIPT_DIR=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
TMP_DIR=$(realpath $SCRIPT_DIR/tmp)
DATA_DIR=$(realpath $SCRIPT_DIR/data)
DOCS_DIR=$(realpath $SCRIPT_DIR/../docs)

pushd $TMP_DIR

# Download the Minecraft Server jar
# https://mcversions.net; 1.21.2-rc2
[ -f server.jar ] || wget https://piston-data.mojang.com/v1/objects/d0601b3f493bb5b4e421ef35b676d4cb9c1f606f/server.jar

# Generate report files (or --all)
java -DbundlerMainClass=net.minecraft.data.Main -jar server.jar --reports

# Copy the items list into data/items.json
mkdir -p $DATA_DIR
cat generated/reports/registries.json | jq '.["minecraft:item"].entries | keys' > $DATA_DIR/items.json

# Copy the items list to the docs site
mkdir -p $DOCS_DIR/data
cp $DATA_DIR/items.json $DOCS_DIR/data/items.json

popd
