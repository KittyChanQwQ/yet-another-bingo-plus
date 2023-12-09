#!/bin/bash

SCRIPT_DIR=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
TMP_DIR=$(realpath $SCRIPT_DIR/tmp)
DATA_DIR=$(realpath $SCRIPT_DIR/data)
DOCS_DIR=$(realpath $SCRIPT_DIR/../docs)

pushd $TMP_DIR

# Download the Minecraft Server jar
# https://mcversions.net; 1.20.4
[ -f server.jar ] || wget https://piston-data.mojang.com/v1/objects/8dd1a28015f51b1803213892b50b7b4fc76e594d/server.jar

# Generate report files (or --all)
java -DbundlerMainClass=net.minecraft.data.Main -jar server.jar --reports

# Copy the items list into data/items.json
mkdir -p $DATA_DIR
cat generated/reports/registries.json | jq '.["minecraft:item"].entries | keys' > $DATA_DIR/items.json

# Copy the items list to the docs site
mkdir -p $DOCS_DIR/data
cp $DATA_DIR/items.json $DOCS_DIR/data/items.json

popd
