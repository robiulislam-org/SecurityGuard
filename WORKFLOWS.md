# GitHub Actions Workflows and Secrets

This repository now contains GitHub Actions workflows to help build APKs and (optionally) deploy Firebase Cloud Functions.

Files added:

- .github/workflows/build-debug-apk.yml — Builds a debug APK on push to main and uploads the APK as a workflow artifact. No secrets required.
- .github/workflows/deploy-firebase-functions.yml — Deploys Cloud Functions from firebase-functions/ to your Firebase project. Requires secrets (see below).

---

Required GitHub Secrets for Firebase deployment (Repository -> Settings -> Secrets & variables -> Actions):

1. FIREBASE_PROJECT_ID
   - Your Firebase project id (for example: my-project-12345)

2. FIREBASE_SERVICE_ACCOUNT
   - Base64-encoded contents of a Google service account JSON key with permissions to deploy Firebase Functions.
   - Create service account in GCP Console with role: Firebase Admin / Cloud Functions Admin (or similar), generate a JSON key, then run:
     ```bash
     base64 service-account.json | pbcopy   # macOS
     base64 service-account.json | xclip -selection clipboard  # Linux (with xclip)
     ```
   - Paste the base64 string into the FIREBASE_SERVICE_ACCOUNT secret value.

Optional secrets for other automations (not required for debug build):

- GOOGLE_SERVICES_JSON_BASE64 — Base64 of `google-services.json` (if you want CI to inject it during builds instead of committing the file locally).
- ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD — if you want CI to produce signed release APKs.

---

How to use:

1. Debug APK build (no secrets needed):
   - Push to main or trigger the workflow manually from Actions -> Build Debug APK.
   - After the run completes, download the artifact named `SecurityGuard-debug-apk`.

2. Deploy Firebase Functions:
   - Add FIREBASE_PROJECT_ID and FIREBASE_SERVICE_ACCOUNT secrets to your repository.
   - Trigger the workflow `Deploy Firebase Functions` from Actions -> Deploy Firebase Functions or push to main.

If you want, I can also add a workflow to build a signed release APK (requires keystore secrets) and/or deploy the web-dashboard to Firebase Hosting (requires hosting setup and the same service account).