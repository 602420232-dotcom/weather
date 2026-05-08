#!/bin/bash
# UAV Edge SDK - Linux/macOS Build Script

set -e

echo "============================================"
echo "  UAV Edge SDK - Build Script"
echo "============================================"
echo ""

# Check if CMake is installed
if ! command -v cmake &> /dev/null; then
    echo "[ERROR] CMake not found!"
    echo "Please install CMake:"
    echo "  Ubuntu/Debian: sudo apt-get install cmake"
    echo "  macOS: brew install cmake"
    echo ""
    exit 1
fi

# Check if C++ compiler is installed
if ! command -v g++ &> /dev/null && ! command -v clang++ &> /dev/null; then
    echo "[ERROR] C++ compiler not found!"
    echo "Please install GCC or Clang:"
    echo "  Ubuntu/Debian: sudo apt-get install build-essential"
    echo "  macOS: brew install gcc"
    echo ""
    exit 1
fi

echo "[INFO] CMake found"
command -v g++ &> /dev/null && echo "[INFO] GCC found" || echo "[INFO] Clang found"
echo ""

# Create build directory
if [ ! -d "build" ]; then
    echo "[INFO] Creating build directory..."
    mkdir -p build
fi

cd build

# Configure CMake
echo "[INFO] Configuring CMake..."
cmake .. -DCMAKE_BUILD_TYPE=Release

# Build
echo ""
echo "[INFO] Building C++ module..."
make -j$(nproc)

# Install
echo ""
echo "[INFO] Installing to python directory..."
make install

echo ""
echo "============================================"
echo "  Build Complete!"
echo "============================================"
echo ""
echo "You can now use the C++ module in Python:"
echo ""
echo "  from edge_sdk import EdgeSDK"
echo "  sdk = EdgeSDK()"
echo ""
echo "To run tests:"
echo ""
echo "  cd tests"
echo "  python test_edge_sdk.py"
echo ""
echo "============================================"
