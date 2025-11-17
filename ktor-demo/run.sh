#!/bin/bash

# Konditional Ktor Demo Launcher
echo "🚀 Starting Konditional Ktor Demo..."
echo "📍 Server will be available at http://localhost:8080"
echo ""

# Navigate to project root and run
cd "$(dirname "$0")/.."
gradle :ktor-demo:run
