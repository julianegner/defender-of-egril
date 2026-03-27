You create these yourself when generating an Android release keystore. Here are the exact steps:

1. Generate a keystore (one-time setup)

keytool -genkeypair \
-keystore release.keystore \
-alias my-key-alias \
-keyalg RSA \
-keysize 2048 \
-validity 10000 \
-storepass myStorePassword \
-keypass myKeyPassword \
-dname "CN=Your Name, OU=Your Org, O=Your Company, L=City, S=State, C=DE"

After running this you will have a release.keystore file.

2. Base64-encode the keystore file

# Linux / macOS
base64 -w 0 release.keystore > release.keystore.b64

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File release.keystore.b64

Copy the contents of release.keystore.b64 – that is your ANDROID_KEYSTORE_BASE64 value.

3. Add the four secrets to the GitHub repository

Go to Settings → Secrets and variables → Actions → New repository secret and add:
Secret name 	Value
ANDROID_KEYSTORE_BASE64 	Contents of release.keystore.b64 (the base64 string)
ANDROID_KEY_ALIAS 	The alias you used (my-key-alias in the example above)
ANDROID_KEY_PASSWORD 	The key password (myKeyPassword)
ANDROID_STORE_PASSWORD 	The store password (myStorePassword)

Important: Keep the release.keystore file and the passwords in a secure location (e.g. a password manager). If you lose them you will not be able to update the app on the Play Store under the same signing identity. Do not commit the keystore file to the repository.

Without these secrets the workflow automatically falls back to an unsigned debug build.
