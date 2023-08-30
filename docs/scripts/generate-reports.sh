#!/bin/bash

# Download the Minecraft Server jar
[ -f server.jar ] || wget https://piston-data.mojang.com/v1/objects/84194a2f286ef7c14ed7ce0090dba59902951553/server.jar

# Generate report files (or --all)
java -DbundlerMainClass=net.minecraft.data.Main -jar server.jar --reports

# Copy the items list into data/items.json
mkdir -p data
cat generated/reports/registries.json | jq '.["minecraft:item"].entries | keys' > ./data/items.json
