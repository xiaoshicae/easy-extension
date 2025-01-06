set -e

VERSION='3.0.1'
TARGET_DIR='../easy-extension-admin-spring-boot-starter/src/main/resources/META-INF/resources/webjars/easy-extension-admin-ui/'${VERSION}

npm run build

rm -rf ${TARGET_DIR:?}/*

cp -r dist/* ${TARGET_DIR}
