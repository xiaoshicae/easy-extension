set -e

VERSION='3.0.2'
TARGET_DIR='../easy-extension-admin-spring-boot-starter/src/main/resources/META-INF/resources/webjars/easy-extension-admin-ui/'${VERSION}

npm run build

echo "Copying dist/* to ${TARGET_DIR}"

mkdir -p ${TARGET_DIR}

if [ "$(ls -A ${TARGET_DIR:?}/)" ]; then
    rm -rf ${TARGET_DIR:?}/*
fi

cp -r dist/* ${TARGET_DIR}
