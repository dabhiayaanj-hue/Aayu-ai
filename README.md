# AAYU AI — Android starter project

A dark blue holographic Android chat UI using the Gemini 3.8 Flash model (`gemini-3.8-flash`). Includes chat history saved locally, speech-to-text input, Android text-to-speech replies, API key settings, quota/error messaging, and an image-picker entry point.

## Build APK using only your Android tablet
1. Sign in to GitHub in your tablet browser and create a new repository named `aayu-ai`.
2. Upload the contents of this folder to the repository (including the `.github` folder). GitHub's web upload flow may require uploading files/folders in a few batches.
3. Open the repository's **Actions** tab and run **Build AAYU AI APK** (or wait for the first push build).
4. Open the successful workflow run, scroll to **Artifacts**, and download `AAYU-AI-debug-apk`.
5. Extract the ZIP on the tablet and install `app-debug.apk`. Android may ask you to allow installs from your browser/files app.
6. Open AAYU Settings and paste your Google AI Studio API key.

## Model
The REST endpoint is `v1beta/models/gemini-3.8-flash:generateContent`. The user-provided label `models/gemini-3.8-flash` is the model resource name; the API model ID used in the URL is `gemini-3.8-flash`.

## Important limitations
- This is a starter implementation, not a finished commercial-grade app. Image selection is present but image bytes are not yet sent to Gemini; true image understanding requires multipart/inline image payload handling.
- Chat history and API key are stored in app preferences on this device. Do not distribute an APK with a personal key saved. For a public app, route API calls through a secure backend, add authentication, and apply rate limits.
- The workflow builds an unsigned debug APK suitable for personal testing, not a signed Play Store release.
- API access and quotas depend on the Google AI Studio project/key. The app displays common quota and connection errors but cannot increase provider quotas.
