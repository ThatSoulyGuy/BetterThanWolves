#!/bin/bash
# Migrates btw/util/ package in Common to use Api interfaces where possible
# Phase 1: Only change parameter/variable types that we have interfaces for
cd "$(dirname "$0")"

echo "Migrating Common/btw/util/ to use Api interfaces..."

UTIL_DIR="Common/src/main/java/net/minecraft/src/btw/util"

for f in "$UTIL_DIR"/*.java; do
    [ ! -f "$f" ] && continue
    base=$(basename "$f")

    # Add Api imports (after the package line, before existing imports)
    if ! grep -q "btw.api.block.IBlock" "$f"; then
        sed -i '/^package /a\
import btw.api.block.IBlock;\
import btw.api.item.IItem;\
import btw.api.item.IItemStack;\
import btw.api.material.IMaterial;\
import btw.api.registry.BlockRegistry;' "$f"
    fi

    echo "  Processed: $base"
done

echo "Done. Build to verify..."
