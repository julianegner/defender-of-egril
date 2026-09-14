# Microsoft Store Publishing – Setup Guide

This guide explains every step needed to publish Defender of Egril to the
**Microsoft Store** using the automated `Publish to Microsoft Store` GitHub
Actions workflow.

---

## Overview

The publishing pipeline consists of two parts:

1. **MSIX build** – runs inside the `Release` workflow as the `build_windows_msix`
   job and attaches the signed `.msix` package to the GitHub Release.
2. **Store submission** – the reusable `publish_windows_store.yml` workflow
   downloads the MSIX from the GitHub Release and submits it to the Microsoft
   Store via the Partner Center Submission API.

The submission job is skipped automatically when the required secrets are not
configured, so the release pipeline never fails because of missing Store
credentials.

---

## Prerequisites

| What you need | Where to get it |
|---|---|
| Microsoft Partner Center account | <https://partner.microsoft.com/dashboard> |
| Azure Active Directory tenant (already included with a Microsoft 365 / Entra ID account) | <https://portal.azure.com> |
| Windows developer account (one-time fee) | <https://learn.microsoft.com/en-us/windows/apps/publish/partner-center/opening-a-developer-account> |

---

## Step 1 – Create a Microsoft Partner Center Account

1. Go to <https://partner.microsoft.com/dashboard> and sign in with a
   Microsoft account.
2. Enroll in the **Windows & Xbox** programme (requires a one-time registration
   fee of approximately USD 19 for an individual account or USD 99 for a
   company account).
3. Complete identity verification.

Reference: <https://learn.microsoft.com/en-us/windows/apps/publish/partner-center/opening-a-developer-account>

---

## Step 2 – Reserve the App Name in Partner Center

1. In Partner Center go to **Apps and games → New product → App**.
2. Enter **Defender of Egril** as the app name and click **Reserve product name**.
3. Note the **Product ID** shown in the URL
   (`https://partner.microsoft.com/…/apps/<PRODUCT_ID>/…`).
   You will need this value as the `WINDOWS_STORE_APP_ID` secret.

---

## Step 3 – Obtain Your Publisher Identity

1. In Partner Center go to **Account settings → Organisation profile**.
2. Find the **Publisher identity** field.  It looks like
   `CN=ABC12345DEFGHIJK` (for individual accounts) or a longer distinguished
   name for company accounts.
3. Save this string – you will use it as the `WINDOWS_STORE_PUBLISHER_ID`
   GitHub secret **and** it must exactly match the `Publisher` attribute in
   `frontend/windowsstore/AppxManifest.xml`.

---

## Step 4 – Register an Azure AD Application

The submission API uses OAuth 2.0 client credentials issued by Azure Active
Directory.

1. Open <https://portal.azure.com> and navigate to
   **Microsoft Entra ID → App registrations → New registration**.
2. Enter a name (e.g. `defender-of-egril-store-ci`) and click **Register**.
3. Note the **Application (client) ID** and **Directory (tenant) ID**.
4. Under **Certificates & secrets → New client secret**, create a secret and
   note the value immediately (it is only shown once).

Reference: <https://learn.microsoft.com/en-us/windows/apps/publish/manage-app-submissions/create-and-manage-submissions-using-windows-store-services#associate-an-azure-ad-application-with-your-partner-center-account>

---

## Step 5 – Link the Azure AD App to Partner Center

1. In Partner Center go to **Account settings → User management →
   Azure AD applications → Add Azure AD application**.
2. Select the Azure AD application created in Step 4.
3. Assign it the **Manager** role (required for submissions).

---

## Step 6 – Create a Code Signing Certificate

The MSIX package must be signed before it is submitted to the Store.  The
Publisher field of the signing certificate must match the Publisher identity
obtained in Step 3.

### Option A – Use the Partner Center certificate (recommended)

1. In Partner Center go to **Product management → MSIX packaging**.
2. Download the `.pfx` or `.p12` signing certificate provided by Partner Center.
3. Convert to Base64:
   ```bash
   base64 -w 0 signing.pfx > signing_b64.txt
   ```
4. Use this Base64 string as the `WINDOWS_STORE_CODE_SIGNING_CERT` secret.
5. Store the certificate password as `WINDOWS_STORE_CODE_SIGNING_CERT_PASSWORD`.

### Option B – Self-signed certificate (testing only)

For local testing you can create a self-signed certificate:

```powershell
New-SelfSignedCertificate `
  -Type Custom `
  -Subject "CN=<Your Publisher Identity>" `
  -KeyUsage DigitalSignature `
  -FriendlyName "Defender of Egril Dev Cert" `
  -CertStoreLocation "Cert:\CurrentUser\My" `
  -TextExtension @("2.5.29.37={text}1.3.6.1.5.5.7.3.3", "2.5.29.19={text}")
```

**A self-signed certificate is NOT accepted by the Microsoft Store.**

---

## Step 7 – Add GitHub Secrets

Go to **Settings → Secrets and variables → Actions → New repository secret**
and add the following secrets:

| Secret name | Description | Where to find it |
|---|---|---|
| `WINDOWS_STORE_TENANT_ID` | Azure AD Directory (tenant) ID | Azure portal → App registration → Overview |
| `WINDOWS_STORE_CLIENT_ID` | Azure AD Application (client) ID | Azure portal → App registration → Overview |
| `WINDOWS_STORE_CLIENT_SECRET` | Azure AD client secret value | Step 4 above (copy immediately after creation) |
| `WINDOWS_STORE_APP_ID` | Partner Center Product ID | Partner Center → your app URL |
| `WINDOWS_STORE_PUBLISHER_ID` | Publisher identity string (e.g. `CN=ABC…`) | Partner Center → Account settings → Organisation profile |
| `WINDOWS_STORE_PUBLISHER_DISPLAY_NAME` | Human-readable publisher name | Partner Center → Account settings |
| `WINDOWS_STORE_CODE_SIGNING_CERT` | Base64-encoded `.pfx` code signing certificate | Step 6 above |
| `WINDOWS_STORE_CODE_SIGNING_CERT_PASSWORD` | Password for the `.pfx` certificate | Set when exporting the certificate |

---

## Step 8 – Update AppxManifest.xml

After obtaining your Publisher identity (Step 3), verify that the
`Identity.Name` attribute in
`frontend/windowsstore/AppxManifest.xml` matches the Package Name reserved in
Partner Center (Step 2).

The workflow will substitute `{{PUBLISHER}}`, `{{PUBLISHER_DISPLAY_NAME}}`, and
`{{VERSION}}` at build time using the GitHub secrets and release version.

---

## Step 9 – First Submission (manual steps)

The first submission must be completed manually in Partner Center to fill in
all required metadata:

1. Upload screenshots (at least 1 for each supported resolution).
2. Write the Store listing (description, features, etc.) in all required
   languages.
3. Set age ratings, pricing, and availability.
4. Submit for certification.

After the first submission is approved, subsequent releases can be submitted
fully automatically by the GitHub Actions workflow.

Reference: <https://learn.microsoft.com/en-us/windows/apps/publish/publish-your-app/create-app-submission>

---

## Workflow Usage

### Automatic (recommended)

The `Release` workflow triggers the `Publish to Microsoft Store` workflow
automatically after every successful release.  No action is needed once secrets
are configured.

### Manual re-run

1. Go to the **Actions** tab.
2. Select **Publish to Microsoft Store**.
3. Click **Run workflow**.
4. Optionally specify a `release_tag` (e.g. `v1.2.3`); leave empty to use
   the latest release.

---

## Troubleshooting

| Error | Likely cause | Fix |
|---|---|---|
| `AADSTS700016` | Azure AD app not found in tenant | Verify `WINDOWS_STORE_TENANT_ID` and `WINDOWS_STORE_CLIENT_ID` |
| `401 Unauthorized` from `manage.devcenter.microsoft.com` | Azure AD app not linked to Partner Center | Repeat Step 5 |
| `Package publisher does not match` | Publisher in AppxManifest.xml ≠ signing certificate Subject | Ensure both use the exact string from Step 3 |
| `No .msix file found in release assets` | `build_windows_msix` job was skipped | Check that `WINDOWS_STORE_PUBLISHER_ID` is set |
| Submission committed but certification fails | Store listing is incomplete | Complete the first manual submission (Step 9) |

---

## Useful Links

- [Partner Center dashboard](https://partner.microsoft.com/dashboard)
- [Submission API documentation](https://learn.microsoft.com/en-us/windows/apps/publish/manage-app-submissions/create-and-manage-submissions-using-windows-store-services)
- [MSIX packaging overview](https://learn.microsoft.com/en-us/windows/msix/overview)
- [Code signing for MSIX](https://learn.microsoft.com/en-us/windows/msix/package/signing-package-overview)
- [Partner Center account types](https://learn.microsoft.com/en-us/windows/apps/publish/partner-center/account-types-locations-and-fees)
