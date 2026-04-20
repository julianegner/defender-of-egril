# Google Play Store Listing

This directory contains all assets and configuration needed to manage the
**Defender of Egril** listing on the Google Play Store.

## Directory Structure

```
googleplay/
├── Gemfile            – Ruby gem dependencies (Fastlane)
├── Fastfile           – Fastlane lanes for uploading metadata
├── README.md          – This file
└── metadata/
    └── android/
        ├── en-US/     – American English
        ├── en-GB/     – British English
        ├── de-DE/     – German
        ├── es-ES/     – Spanish
        ├── fr-FR/     – French
        └── it-IT/     – Italian
```

Each locale directory contains:

| File | Max length | Description |
|------|-----------|-------------|
| `title.txt` | 50 characters | App title shown on the Play Store |
| `short_description.txt` | 80 characters | Brief tagline shown in search results |
| `full_description.txt` | 4 000 characters | Full app description |
| `changelogs/default.txt` | 500 characters | Default release notes (overridden per version code) |

## Uploading the Store Listing

The GitHub Actions workflow (`.github/workflows/deploy-play-store-listing.yml`)
uploads the metadata automatically. You can also run it locally with
[Fastlane](https://fastlane.tools/).

### Local usage

```bash
# From this directory
cd frontend/googleplay
bundle install
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON="$(cat /path/to/service-account.json)" \
  bundle exec fastlane upload_listing
```

## Required Secrets

You must add the following secret to the GitHub repository
(**Settings → Secrets and variables → Actions → New repository secret**):

| Secret name | Description |
|-------------|-------------|
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Full JSON content of a Google Play service account key that has **Edit store listing** permission for the `de.egril.defender` package. |

### How to create the service account

1. Open the [Google Play Console](https://play.google.com/console) and go to
   **Setup → API access**.
2. Link your Play Console project to a **Google Cloud project** (or create one).
3. In the Google Cloud Console, open **IAM & Admin → Service Accounts** and
   create a new service account.
4. Create a JSON key for the service account and download it.
5. Back in the Play Console, go to **Users and permissions → Invite new users**,
   enter the service account email, and grant it the
   **Release manager** or **Edit store listing** role for your app.
6. Copy the entire contents of the downloaded JSON key file and store them as
   the `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` repository secret.

For a detailed walkthrough, see the official guide:
<https://developers.google.com/android-publisher/getting_started>

## Adding a New Language

1. Create a new directory under `metadata/android/` using the BCP 47 locale
   code supported by Google Play (e.g. `pt-BR` for Brazilian Portuguese).
2. Add the four files: `title.txt`, `short_description.txt`,
   `full_description.txt`, and `changelogs/default.txt`.
3. Commit and push – the workflow will pick up the new locale automatically.

## Adding Per-Release Changelogs

For each new release, add a file named after the Android `versionCode`
(e.g. `10300.txt` for version 1.3.0) inside the `changelogs/` folder of
every locale you want to update. If a version-specific file is absent,
Fastlane falls back to `default.txt`.

```
changelogs/
├── default.txt   – fallback for any version without a specific file
└── 10300.txt     – changelog for versionCode 10300 (= version 1.3.0)
```
