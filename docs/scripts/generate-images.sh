#!/bin/bash
set -e

VERSION="1.20.1"
JAR_FILE="$(realpath ~/.minecraft/versions/$VERSION/*.jar)"

[ -f $JAR_FILE ] || {
  echo "Error: minecraft jar for $VERSION not found."
  exit 1
}

ITEMS_FILE="$(realpath ./data/items.json)"
[ -f $ITEMS_FILE ] || {
  echo "Error: items.json missing. Run scripts/generate-reports.sh first!"
  exit 1
}

IMAGE_MASK_DIR="$(realpath ./data/masks)"

OUT_DIR="$(realpath ../src/main/resources/bingo/images)"
mkdir -p $OUT_DIR

TMP_DIR=/tmp/mc-generate-images
mkdir -p $TMP_DIR
pushd $TMP_DIR

# Copy a texture identifier directly to the output dir
# $1 = texture ("minecraft:block/oak_planks")
# $2 = item ("oak_planks")
function copy_texture {
  texture_id=$(echo $1 | xargs | rev | cut -d ':' -f1 | rev)
  texture_file=./assets/minecraft/textures/$texture_id.png

  [ -f $texture_file ] || {
    echo "Error: texture file for $texture_file does not exist!"
    exit 1
  }

  # copy the texture to the out_dir
  cp -f $texture_file $OUT_DIR/$(echo $2 | xargs).png
}

# Apply a mask image to the texture identifier before writing to the output dir
# $1 = texture ("minecraft:block/oak_planks")
# $2 = mask image (/home/.../data/masks/image.png)
# $3 = item ("oak_planks")
function mask_texture {
  texture_id="$(echo $1 | xargs | rev | cut -d ':' -f1 | rev)"
  texture_file=./assets/minecraft/textures/${texture_id}.png

  [ -f $texture_file ] || {
    echo "Error: texture file for $texture_id does not exist!"
    exit 1
  }

  # mask the image with ImageMagick, and write to the output dir
  # https://stackoverflow.com/a/22417663
  convert $texture_file \( $2 \) -compose CopyOpacity -composite PNG32:$OUT_DIR/$(echo $3 | xargs).png
}

# Apply a color tint to the texture identifier before writing it to the output dir
# $1 = texture ("minecraft:block/oak_planks")
# $2 = color (#ffffff)
# $3 = item ("oak_planks")
function color_texture {
  texture_id="$(echo $1 | xargs | rev | cut -d ':' -f1 | rev)"
  texture_file=./assets/minecraft/textures/${texture_id}.png

  [ -f $texture_file ] || {
    echo "Error: texture file for $texture_id does not exist!"
    exit 1
  }

  # tint the image with ImageMagick, and write to the output dir
  # https://www.imagemagick.org/discourse-server/viewtopic.php?t=33380
  # convert $texture_file -colorspace gray -fill $2 -tint 100 PNG32:$OUT_DIR/$(echo $3 | xargs).png
  convert $texture_file \( xc:$(echo $2 | xargs) -scale 16x16 \) -compose multiply -channel rgb -composite PNG32:$OUT_DIR/$(echo $3 | xargs).png
}

# extract jar assets (into ./assets)
jar -xf $JAR_FILE assets

# loop over items registry
for item in $(jq -r ".[]" $ITEMS_FILE); do
  item_id="$(echo $item | cut -d ':' -f2)"
  echo -n "${item_id}: "

  item_model_file=./assets/minecraft/models/item/"${item_id}.json"
  [ -f $item_model_file ] || {
    echo "Warning: item model for $item_id not found"
    continue
  }

  # temporarily copy the new item model to tmp.json
  cp -f $item_model_file tmp.json

  # collect an array of all item models encountered in the hierarchy
  item_models="[]"

  # resolve all model parents, up to a generated model
  while item_parent="$(jq -r '.parent' tmp.json)" && [ "$item_parent" != "null" ] && [ "$item_parent" != "block/block" ] && [ "$item_parent" != "builtin/generated" ] && [ "$item_parent" != "builtin/entity" ]; do
    # locate the next item parent file
    item_parent_id=$(echo $item_parent | cut -d ':' -f2)
    item_parent_file=./assets/minecraft/models/${item_parent_id}.json
    [ -f $item_parent_file ] || {
      echo "Warning: parent model for $item_parent not found"
      exit 1
    }

    # add the item parent to the item_models array
    item_models=$(echo $item_models | jq ". + [\"$item_parent\"]")

    # merge the model with its parent
    item_model_json="$(jq -s '.[0] * .[1]' tmp.json $item_parent_file)" || {
      echo "Error: could not resolve model parent $item_parent_id from $item_id"
      exit 1
    }

    # overwrite tmp.json
    echo "$item_model_json" > tmp.json
  done

  # add the last item_parent to item_models
  item_models=$(echo $item_models | jq ". + [\"$item_parent\"]")

  # if the item is a block
  if [ "$item_parent" == "block/block" ]; then
    # try to find its side texture
    # ("beacon" for beacon blocks) ("pattern" for glazed terracotta) ("wool" for carpet models) ("wall" for wall models) (prefer "side" and "west" before top textures)
    block_texture=$(jq ".textures | (.end_rod // .beacon // .pattern // .wool // .wall // .texture // .all // .front // .side // .west // .top // .up // .flower)" tmp.json)

    [ "$block_texture" == "null" ] && {
      echo "Error: block texture is null"
      echo $item_models
      cat tmp.json
      exit 1
    }

    # see if any image masks are included in the item models
    image_mask_id="null"
    image_mask_file="null"
    for image_mask in $IMAGE_MASK_DIR/*; do
      image_mask_id="$(basename $image_mask | cut -d '.' -f1 | sed 's/\-/\//g')"
      if echo $item_models | jq -e ". | index( \"$image_mask_id\" )" >/dev/null; then
        image_mask_file=$image_mask
        break
      fi
    done

    # hack for bamboo fence & fence gate; these have a messy texture map that doesn't work well with the image masks
    if [ "$item_id" == "bamboo_fence" ] || [ "$item_id" == "bamboo_fence_gate" ]; then
      echo "USING MASK $image_mask_id: minecraft:block/bamboo_planks"
      mask_texture "minecraft:block/bamboo_planks" $image_mask_file $item_id
      continue
    fi

    # if there is an image mask, write the masked texture
    [ "$image_mask_file" != "null" ] && {
      echo "USING MASK $image_mask_id: $block_texture"
      mask_texture $block_texture $image_mask_file $item_id
      continue
    }

    # if it's a leaf block, we need to apply the colormap: #48b518.
    if echo $item_models | jq -e '. | index( "minecraft:block/leaves" )' >/dev/null; then
      echo "USING COLOR TINT: $block_texture"
      color_texture $block_texture "#48b518" $item_id
      continue
    fi

    # otherwise, copy the full block texture
    echo -n "$block_texture :"
    echo $item_models
    copy_texture $block_texture $item_id
    continue
  fi

  # if the item is an item
  if [ "$item_parent" == "builtin/generated" ]; then
    item_texture=$(jq ".textures.layer0" tmp.json)

    [ "$item_texture" == "null" ] && {
      echo "Error: item texture is null"
      echo $item_models
      cat tmp.json
      exit 1
    }

    # if it's a grass/fern block, we need to apply the colormap: #48b518.
    if [ "$item_id" == "grass" ] || [ "$item_id" == "tall_grass" ] || [ "$item_id" == "fern" ] || [ "$item_id" == "large_fern" ]; then
      # some items (leaves / grass / ferns) have color masks that need to be applied... #48b518.
      echo -n "USING COLOR TINT: $item_texture"
      color_texture $item_texture "#48b518" $item_id
      continue
    fi

    # copy the texture with no modifications
    echo $item_texture
    copy_texture $item_texture $item_id
    continue
  fi

  # if the item is a banner
  if echo $item_models | jq -e '. | index( "minecraft:item/template_banner" )' >/dev/null; then
    item_color=${item_id%_banner}
    # replace some unsupported item colors with ImageMagick ids
    [ "$item_color" == "dark_green" ] && item_color="green4"
    [ "$item_color" == "light_blue" ] && item_color="#8db4f0"
    [ "$item_color" == "light_gray" ] && item_color="gray80"
    [ "$item_color" == "brown" ] && item_color="#602b04"

    echo "BANNER TINT: $item_color"
    # convert $IMAGE_MASK_DIR/template_banner.png -colorspace gray -fill $item_color -tint 300 PNG32:$OUT_DIR/$(echo $item_id | xargs).png
    convert $IMAGE_MASK_DIR/template_banner.png \( +clone -colorspace gray -function polynomial -4,4,0 -background $item_color -alpha shape \) -channel rgb -composite PNG32:$OUT_DIR/$(echo $item_id | xargs).png
    continue
  fi

  # if the item is a bed
  if echo $item_models | jq -e '. | index( "minecraft:item/template_bed" )' >/dev/null; then
    item_color=${item_id%_bed}
    # replace some unsupported item colors with ImageMagick ids
    [ "$item_color" == "dark_green" ] && item_color="green4"
    [ "$item_color" == "light_blue" ] && item_color="#8db4f0"
    [ "$item_color" == "light_gray" ] && item_color="gray80"
    [ "$item_color" == "brown" ] && item_color="#602b04"

    echo "BED TINT: $item_color"
    # convert $IMAGE_MASK_DIR/template_bed.png -colorspace gray -fill $item_color -tint 300 PNG32:$OUT_DIR/$(echo $item_id | xargs).png
    # convert $IMAGE_MASK_DIR/template_bed.png \( xc:$item_color -scale 16x16 \) -compose multiply -channel rgb -composite PNG32:$OUT_DIR/$(echo $item_id | xargs).png
    convert $IMAGE_MASK_DIR/template_bed.png \( +clone -colorspace gray -function polynomial -4,4,0 -background $item_color -alpha shape \) -channel rgb -composite PNG32:$OUT_DIR/$(echo $item_id | xargs).png
    continue
  fi

  # otherwise, try to find an image file that matches...
  fallback_image=./assets/minecraft/textures/block/$item_id.png
  [ -f $fallback_image ] && cp -f $fallback_image $OUT_DIR/$(echo $item_id | xargs).png

  fallback_image=./assets/minecraft/textures/item/$item_id.png
  [ -f $fallback_image ] && cp -f $fallback_image $OUT_DIR/$(echo $item_id | xargs).png

  echo "FALLBACK"
  # TODO: list items making it here:
  # - zombie_head
  # - anything under template_skull (wither_skull, skeleton_skull)
  # - decorated_pot
  # - conduit
  # - chest

  if [ "$item_parent" == "null" ]; then
    echo AIR
  fi
done

popd
rm -rf $TMP_DIR
