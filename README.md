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

# Disclaimer

The contents of this repository are subject to revision. No representation or warranty, express or implied, is provided in relation to the accuracy, correctness, completeness, or reliability of the information, opinions, or conclusions expressed in this repository.

The contents of this repository (the “Materials”) are experimental in nature and should be used with prudence and appropriate caution. Any use is voluntary and at your own risk.

The Materials are made available “as is” by USC (the University of Southern California and its trustees, directors, officers, employees, faculty, students, agents, affiliated organizations and their insurance carriers, if any), its collaborators Med-Ally LLC and Medipace Inc., and any other contributors to this repository (collectively, “Providers”). Providers expressly disclaim all warranties, express or implied, arising in connection with the Materials, or arising out of a course of performance, dealing, or trade usage, including, without limitation, any warranties of title, noninfringement, merchantability, or fitness for a particular purpose.

Any user of the Materials agrees to forever indemnify, defend, and hold harmless Providers from and against, and to waive any and all claims against Providers for any and all claims, suits, demands, liability, loss or damage whatsoever, including attorneys' fees, whether direct or consequential, on account of any loss, injury, death or damage to any person or (including without limitation all agents and employees of Providers and all property owned by, leased to or used by either Providers) or on account of any loss or damages to business or reputations or privacy of any persons, arising in whole or in part in any way from use of the Materials or in any way connected therewith or in any way related thereto.

The Materials, any related materials, and all intellectual property therein, are owned by Providers.
