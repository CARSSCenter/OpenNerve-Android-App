# OpenNerve-Android-App
The source code for an Android app to interact with OpenNerve IPGs and Development boards.

## Setup

### Private Keys

This project requires three private key files that are **not** included in the repository for security reasons. Before building, you must place the following files in `app/src/main/res/raw/`:

- `privatekey_pkcs8_admin.der`
- `privatekey_pkcs8_clinician.der`
- `privatekey_pkcs8_patient.der`

These files must be in PKCS#8 DER format. A set of development keys is available upon request from the project maintainer. For any new clinical application, we recommend generating your own key pairs (see below).

## Generating New Key Pairs

The OpenNerve system uses ECDSA P-256 public-key cryptography. The IPG firmware holds public keys for three permission levels — Admin, Clinician, and Patient — and the Android app must send the corresponding private key during the BLE startup sequence to authenticate. Public keys can be shared openly; private keys **must** be kept secure.

You can generate new key pairs using [OpenSSL](https://github.com/openssl/openssl/releases), which is pre-installed on Mac and Linux. The steps below show how to generate an Admin key pair. Repeat with different filenames for Clinician and Patient keys.

### 1. Generate a P-256 private key

```
openssl ecparam -name prime256v1 -genkey -noout -out admin_priv.pem
```

### 2. Extract the public key

```
openssl ec -in admin_priv.pem -pubout -out admin_pub.pem
```

### 3. Convert the private key to PKCS#8 DER format (required by this app)

```
openssl pkcs8 -topk8 -nocrypt -in admin_priv.pem -outform DER -out privatekey_pkcs8_admin.der
```

### 4. Convert the public key to DER format

```
openssl ec -in admin_priv.pem -pubout -outform DER -out publickey_admin.der
```

### 5. (Optional) Print keys to terminal for inspection

```
openssl ec -in admin_priv.pem -pubout -text -noout
```

Place the resulting `privatekey_pkcs8_*.der` files in `app/src/main/res/raw/` before building the app. The corresponding public key DER files must also be placed in the same directory (the repository already includes the development public keys).

If you generate new keys, you will also need to update the IPG firmware's public keys to match. See the [OpenNerve IPG encryption guide](https://github.com/coforcemed/OpenNerve-Implantable-Pulse-Generator/blob/main/Docs/encryption-instructions.md) for details on updating firmware.
