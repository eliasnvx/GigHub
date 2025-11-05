#!/bin/bash

# GigHub Quick Development Script for Windsurf

echo "🚀 GigHub Quick Dev Mode"
echo ""

# Функция для сборки и копирования
build_and_copy() {
    echo "🔨 Building..."
    export JAVA_HOME=$(/usr/libexec/java_home -v 21)
    mvn clean package -q
    
    if [ $? -eq 0 ]; then
        echo "📦 Copying to test server..."
        mkdir -p test-server/plugins
        cp gighub-bukkit/target/GigHub-*.jar test-server/plugins/
        echo "✅ Done! Restart server to apply."
    else
        echo "❌ Build failed!"
    fi
}

# Проверяем аргументы
case "$1" in
    build)
        build_and_copy
        ;;
    watch)
        echo "👀 Watching for changes... (Press Ctrl+C to stop)"
        if ! command -v fswatch &> /dev/null; then
            echo "⚠️  fswatch not found. Install with: brew install fswatch"
            exit 1
        fi
        fswatch -o gighub-core/src gighub-bukkit/src | while read f; do
            echo ""
            echo "🔄 Changes detected at $(date '+%H:%M:%S')"
            build_and_copy
        done
        ;;
    server)
        if [ ! -f "test-server/paper.jar" ]; then
            echo "❌ Test server not found!"
            echo "Run ./setup-test-server.sh first"
            exit 1
        fi
        echo "🎮 Starting test server with debug..."
        cd test-server
        export JAVA_HOME=$(/usr/libexec/java_home -v 21)
        java -Xms2G -Xmx2G -XX:+UseG1GC \
          -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
          -jar paper.jar --nogui
        ;;
    *)
        echo "Usage: $0 {build|watch|server}"
        echo ""
        echo "Commands:"
        echo "  build  - Build and copy plugin once"
        echo "  watch  - Watch for changes and auto-rebuild"
        echo "  server - Start test server with debug enabled"
        echo ""
        echo "Example workflow:"
        echo "  Terminal 1: ./quick-dev.sh watch"
        echo "  Terminal 2: ./quick-dev.sh server"
        ;;
esac
