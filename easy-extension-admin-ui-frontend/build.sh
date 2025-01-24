set -e

# package.json, src/service-worker.js need update version also.
VERSION='3.1.1'
EASY_EXTENSION_ADMIN_UI_DIR='../easy-extension-admin-spring-boot-starter/src/main/resources/META-INF/resources/webjars/easy-extension-admin-ui'
TARGET_DIR=${EASY_EXTENSION_ADMIN_UI_DIR}/${VERSION}

echo "Building ui to dist/* with version" $VERSION

npm run build

echo "Cleaning EASY_EXTENSION_ADMIN_UI_DIR ${EASY_EXTENSION_ADMIN_UI_DIR}"

if [ "$(ls -A ${EASY_EXTENSION_ADMIN_UI_DIR:?}/)" ]; then
    rm -rf ${EASY_EXTENSION_ADMIN_UI_DIR:?}/*
fi

echo "Copying dist/* to ${TARGET_DIR}"

mkdir -p ${TARGET_DIR}

cp -r dist/* ${TARGET_DIR}
