#!/usr/bin/env bash
set -euo pipefail

# Point Gradle at the SDK installed in the image
echo "sdk.dir=$ANDROID_HOME" > /workspace/local.properties

chmod +x /workspace/gradlew

# ---- signing keystore ----
KEYSTORE="${KEYSTORE_FILE:-/tmp/swiss-release.jks}"
ALIAS="${KEY_ALIAS:-swiss}"
STORE_PASS="${STORE_PASSWORD:-swiss123456}"
KEY_PASS="${KEY_PASSWORD:-$STORE_PASS}"

if [ ! -f "$KEYSTORE" ]; then
    echo "[swiss] No keystore found — generating a temporary one at $KEYSTORE"
    keytool -genkeypair \
        -keystore "$KEYSTORE" \
        -alias "$ALIAS" \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
        -dname "CN=Swiss VPN, O=Swiss, C=US" \
        -noprompt
fi

# ---- build ----
echo "[swiss] Building release APK…"
./gradlew assembleRelease --no-daemon

# ---- sign ----
UNSIGNED=$(find app/build/outputs/apk/release -name "*unsigned*.apk" | head -1)
SIGNED=$(find app/build/outputs/apk/release -name "*.apk" ! -name "*unsigned*" | head -1)

mkdir -p /output

if [ -n "$UNSIGNED" ]; then
    OUT_APK="/output/swiss-release.apk"
    echo "[swiss] Signing $UNSIGNED → $OUT_APK"
    apksigner sign \
        --ks "$KEYSTORE" \
        --ks-key-alias "$ALIAS" \
        --ks-pass "pass:$STORE_PASS" \
        --key-pass "pass:$KEY_PASS" \
        --out "$OUT_APK" \
        "$UNSIGNED"
elif [ -n "$SIGNED" ]; then
    cp "$SIGNED" /output/swiss-release.apk
else
    echo "[swiss] ERROR: no APK found in app/build/outputs/apk/release/" >&2
    exit 1
fi

echo "[swiss] Done:"
ls -lh /output/swiss-release.apk
