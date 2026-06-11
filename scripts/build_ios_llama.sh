#!/usr/bin/env bash
# Build llama.cpp as an iOS xcframework with Metal acceleration.
# Run on macOS with Xcode + CMake installed. NOT runnable in the Linux sandbox.
set -euo pipefail

LLAMA_DIR="${1:-llama.cpp}"
OUT_DIR="${2:-build-ios}"
DEPLOY_TARGET="15.0"

if [[ ! -d "$LLAMA_DIR" ]]; then
  echo "llama.cpp not found at $LLAMA_DIR (run: git submodule update --init)"; exit 1
fi

build_slice() {
  local name="$1"; local sysname="$2"; local sysroot="$3"; local archs="$4"
  local bdir="$OUT_DIR/$name"
  cmake -S "$LLAMA_DIR" -B "$bdir" -G Xcode \
    -DCMAKE_SYSTEM_NAME="$sysname" \
    -DCMAKE_OSX_ARCHITECTURES="$archs" \
    -DCMAKE_OSX_SYSROOT="$sysroot" \
    -DCMAKE_OSX_DEPLOYMENT_TARGET="$DEPLOY_TARGET" \
    -DBUILD_SHARED_LIBS=OFF \
    -DGGML_METAL=ON \
    -DGGML_METAL_EMBED_LIBRARY=ON \
    -DGGML_OPENMP=OFF \
    -DLLAMA_BUILD_TESTS=OFF \
    -DLLAMA_BUILD_EXAMPLES=OFF \
    -DLLAMA_BUILD_SERVER=OFF \
    -DLLAMA_BUILD_COMMON=ON
  cmake --build "$bdir" --config Release -j
}

# Device (arm64) and simulator (arm64) slices.
build_slice "ios-arm64"      "iOS"           "iphoneos"        "arm64"
build_slice "ios-sim-arm64"  "iOS"           "iphonesimulator" "arm64"

# Collect the static archives per slice into flat dirs (lib*.a from ggml + llama).
for slice in ios-arm64 ios-sim-arm64; do
  mkdir -p "$OUT_DIR/libs/$slice"
  find "$OUT_DIR/$slice" -name 'lib*.a' -exec cp {} "$OUT_DIR/libs/$slice/" \;
done

# Build a single fat static lib per slice (libtool) so the xcframework has one lib.
for slice in ios-arm64 ios-sim-arm64; do
  libtool -static -o "$OUT_DIR/libs/$slice/libllama_all.a" "$OUT_DIR/libs/$slice"/lib*.a
done

rm -rf llama.xcframework
xcodebuild -create-xcframework \
  -library "$OUT_DIR/libs/ios-arm64/libllama_all.a"     -headers "$LLAMA_DIR/include" \
  -library "$OUT_DIR/libs/ios-sim-arm64/libllama_all.a" -headers "$LLAMA_DIR/include" \
  -output llama.xcframework

echo "Built llama.xcframework. Enable the cinterop block in shared/core/build.gradle.kts."
