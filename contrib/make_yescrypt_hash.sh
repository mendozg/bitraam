#!/bin/bash

LIBYESCRYPT_VERSION="627b36796e50435cf32693a0efb64eaa1b99a303"
# ^ tag v0.5.0

set -e

. "$(dirname "$0")/build_tools_util.sh" || (echo "Could not source build_tools_util.sh" && exit 1)

here="$(dirname "$(realpath "$0" 2> /dev/null || grealpath "$0")")"
CONTRIB="$here"
PROJECT_ROOT="$CONTRIB/.."

pkgname="yescrypt_hash"
info "Building $pkgname..."

(
    cd "$CONTRIB"
    if [ ! -d yescrypt_hash ]; then
        git clone https://github.com/mendozg/yescrypt_hash.git
    fi
    cd yescrypt_hash
    if ! $(git cat-file -e ${LIBYESCRYPT_VERSION}) ; then
        info "Could not find requested version $LIBYESCRYPT_VERSION in local clone; fetching..."
        git fetch --all
    fi
    git reset --hard
    git clean -dfxq
    git checkout "${LIBYESCRYPT_VERSION}^{commit}"

    if [ "$BUILD_TYPE" = "wine" ] ; then
        echo "LDFLAGS += -Wc,-static" >> Makefile.am
    fi

    autoreconf -fi || fail "Could not run autoreconf."
    ./configure --host=${GCC_TRIPLET_HOST} --prefix="$here/$pkgname/dist"

    make "-j$CPU_COUNT" || fail "Could not build $pkgname"
    make install || warn "Could not install $pkgname"
    . "$here/$pkgname/dist/lib/libyescrypthash.la"
    host_strip "$here/$pkgname/dist/lib/$dlname"
    TARGET_NAME="$dlname"
    echo "Built $pkgname binary: $dlname"
    if [ $(uname) == "Darwin" ]; then  
        TARGET_NAME="libyescrypthash.dylib"
    fi

    if [ -n "$DLL_TARGET_DIR" ] ; then
        cp -fpv "$here/$pkgname/dist/lib/$dlname" "$DLL_TARGET_DIR/" || fail "Could not copy the $pkgname binary to DLL_TARGET_DIR"
    else
        cp -fpv "$here/$pkgname/dist/lib/$dlname" "$PROJECT_ROOT/bitraam/$TARGET_NAME" || fail "Could not copy the $pkgname binary to its destination"
        info "$TARGET_NAME has been placed in the inner 'bitraam' folder."
    fi
)