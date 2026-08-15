#!/bin/bash

set -e

REPO="https://github.com/raphaelschuhmann/gvw.git"
BRANCH="main"
REPO_DIR="/opt/gvw/repo"
FRONTEND_DIR="/var/www/gvw-office"
BACKEND_DIR="/opt/gvw/backend"

echo "Deploying latest commit from $BRANCH"

if [ ! -d "$REPO_DIR/.git" ]; then
    echo "Repository not found. Cloning..."
    git clone --depth 1 --branch "$BRANCH" "$REPO" "$REPO_DIR"
else
    echo "Repository exists. Updating..."
    cd "$REPO_DIR"
    git fetch --depth 1 origin "$BRANCH"
    git reset --hard "origin/$BRANCH"
fi

echo "Building frontend..."

cd "$REPO_DIR/gvw-office/frontend"

echo "Installing dependencies..."
npm install

echo "Removing previous build..."
rm -rf dist

echo "Building frontend..."
npm run build

echo "Deploying frontend..."
rm -rf "$FRONTEND_DIR"/*
cp -r dist/* "$FRONTEND_DIR/"

echo "Frontend deployed successfully"

echo "Building backend..."

cd "$REPO_DIR/gvw-office/gvw-backend"

echo "Building with Maven..."
./mvnw clean package

echo "Locating generated JAR..."

JAR=$(find target -maxdepth 1 -type f -name 'gvw-backend-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)

if [ -z "$JAR" ]; then
    echo "ERROR: Backend JAR could not be found."
    exit 1
fi

echo "Deploying $JAR..."

cp "$JAR" "$BACKEND_DIR/gvw-backend.jar"

echo "Restarting backend..."
systemctl restart gvw-backend

echo "Backend deployed successfully"

echo "========================================"
echo "Deployment completed successfully"
echo "========================================"

exit 0