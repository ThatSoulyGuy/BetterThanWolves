#!/usr/bin/env bash
#
# BetterThanWolves Build Setup Script
#
# This script automates the setup of the BTW build environment:
#   1. Downloads MCP 7.51 (for Minecraft 1.5.2)
#   2. Decompiles vanilla Minecraft using MCP
#   3. Copies the decompiled source into vanilla/{client,server}/
#   4. Applies BTW patches to produce the modified vanilla source
#
# Prerequisites:
#   - Java 8 (JDK) on PATH
#   - minecraft.jar (1.5.2 client) and minecraft_server.jar (1.5.2)
#     placed in the locations described below
#   - Internet connection (for downloading MCP)
#
# Usage:
#   ./setup.sh [path-to-mcp-workspace]
#
#   If you already have an MCP workspace with decompiled source, pass its path
#   as the first argument to skip the MCP download and decompilation steps.
#

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
MCP_VERSION="7.51"
MCP_URL="https://github.com/ModCoderPack/mcpc/archive/refs/heads/master.zip"

echo "=== Better Than Wolves Build Setup ==="
echo ""

# ---------------------------------------------------------------------------
# Option A: Use an existing MCP workspace
# ---------------------------------------------------------------------------
if [ "${1:-}" != "" ]; then
    MCP_DIR="$1"
    echo "Using existing MCP workspace: $MCP_DIR"
else
    echo "No MCP workspace provided."
    echo ""
    echo "To set up the build environment, you need MCP-decompiled vanilla"
    echo "Minecraft 1.5.2 source code. There are two ways to get this:"
    echo ""
    echo "  Option A: Provide an existing MCP workspace"
    echo "    ./setup.sh /path/to/mcp"
    echo ""
    echo "  Option B: Use RetroMCP (recommended)"
    echo "    1. Install Python 3"
    echo "    2. pip install retromcp"
    echo "    3. mkdir mcp && cd mcp"
    echo "    4. retromcp setup 1.5.2"
    echo "    5. retromcp decompile"
    echo "    6. cd .. && ./setup.sh ./mcp"
    echo ""
    echo "  Option C: Use classic MCP 7.51"
    echo "    1. Download MCP 7.51 from the MCP archive"
    echo "    2. Place minecraft.jar (1.5.2) in mcp/jars/bin/"
    echo "    3. Place minecraft_server.jar (1.5.2) in mcp/jars/"
    echo "    4. Run: cd mcp && python runtime/decompile.py"
    echo "    5. cd .. && ./setup.sh ./mcp"
    echo ""
    exit 1
fi

# ---------------------------------------------------------------------------
# Locate MCP decompiled source
# ---------------------------------------------------------------------------
# MCP puts decompiled source in different locations depending on the version.
# Try common paths.

CLIENT_SRC=""
SERVER_SRC=""

for candidate in \
    "$MCP_DIR/src/minecraft" \
    "$MCP_DIR/src/minecraft/net/minecraft" \
    "$MCP_DIR/src/main/java" \
; do
    if [ -d "$candidate" ]; then
        # Check if this directory has net/minecraft/src/ structure
        if [ -d "$candidate/net/minecraft/src" ]; then
            CLIENT_SRC="$candidate"
            break
        elif [ -d "$candidate/src" ]; then
            CLIENT_SRC="$candidate/.."
            break
        fi
    fi
done

for candidate in \
    "$MCP_DIR/src/minecraft_server" \
    "$MCP_DIR/src/minecraft_server/net/minecraft" \
; do
    if [ -d "$candidate" ]; then
        if [ -d "$candidate/net/minecraft/src" ]; then
            SERVER_SRC="$candidate"
            break
        elif [ -d "$candidate/src" ]; then
            SERVER_SRC="$candidate/.."
            break
        fi
    fi
done

if [ -z "$CLIENT_SRC" ]; then
    echo "ERROR: Could not find decompiled client source in MCP workspace."
    echo "Expected: $MCP_DIR/src/minecraft/net/minecraft/src/"
    exit 1
fi

echo "Found client source: $CLIENT_SRC"
if [ -n "$SERVER_SRC" ]; then
    echo "Found server source: $SERVER_SRC"
fi

# ---------------------------------------------------------------------------
# Copy vanilla source
# ---------------------------------------------------------------------------
echo ""
echo "Copying vanilla source to vanilla/ ..."

rm -rf "$PROJECT_DIR/vanilla/client" "$PROJECT_DIR/vanilla/server"
mkdir -p "$PROJECT_DIR/vanilla/client" "$PROJECT_DIR/vanilla/server"

cp -r "$CLIENT_SRC/"* "$PROJECT_DIR/vanilla/client/"
echo "  Copied client source"

if [ -n "$SERVER_SRC" ]; then
    cp -r "$SERVER_SRC/"* "$PROJECT_DIR/vanilla/server/"
    echo "  Copied server source"
else
    # If no separate server source, use client source as base
    cp -r "$CLIENT_SRC/"* "$PROJECT_DIR/vanilla/server/"
    echo "  Copied client source as server base (no separate server source found)"
fi

# ---------------------------------------------------------------------------
# Apply BTW patches
# ---------------------------------------------------------------------------
echo ""
echo "Applying BTW patches ..."

apply_patch() {
    local patch_file="$1"
    local target_dir="$2"
    local label="$3"

    if [ ! -f "$patch_file" ]; then
        echo "  Skipping $label (no patch file)"
        return
    fi

    echo "  Applying $label ..."
    cd "$target_dir"
    patch -R -p0 --binary -i "$patch_file" || {
        echo "  WARNING: Some hunks from $label may have failed"
    }
    cd "$PROJECT_DIR"
}

apply_patch "$PROJECT_DIR/Src/patch.txt"       "$PROJECT_DIR/vanilla/client" "shared patches"
apply_patch "$PROJECT_DIR/SrcClient/patch.txt"  "$PROJECT_DIR/vanilla/client" "client-only patches"
apply_patch "$PROJECT_DIR/SrcServer/patch.txt"  "$PROJECT_DIR/vanilla/server" "server patches"

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
echo ""
echo "=== Setup complete! ==="
echo ""
echo "You can now build with:"
echo "  ./gradlew jar          # Build client JAR"
echo "  ./gradlew serverJar    # Build server JAR"
echo "  ./gradlew buildAll     # Build both"
echo ""
echo "The compiled JARs will be in build/libs/"
