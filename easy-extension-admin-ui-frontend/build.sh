set -e

# Always output to 'latest' directory to avoid version mismatch issues
EASY_EXTENSION_ADMIN_UI_DIR='../easy-extension-admin-spring-boot-starter/src/main/resources/META-INF/resources/webjars/easy-extension-admin-ui'
TARGET_DIR=${EASY_EXTENSION_ADMIN_UI_DIR}/latest

echo "Installing dependencies..."

npm install --legacy-peer-deps

echo "Building ui to dist/* (will be copied to 'latest' directory)"

npm run build

echo "Cleaning EASY_EXTENSION_ADMIN_UI_DIR ${EASY_EXTENSION_ADMIN_UI_DIR}"

if [ "$(ls -A ${EASY_EXTENSION_ADMIN_UI_DIR:?}/)" ]; then
    rm -rf ${EASY_EXTENSION_ADMIN_UI_DIR:?}/*
fi

echo "Copying dist/* to ${TARGET_DIR}"

mkdir -p ${TARGET_DIR}

cp -r dist/* ${TARGET_DIR}

echo "Build complete. Admin UI is available at: /latest/"
