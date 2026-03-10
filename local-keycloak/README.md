# Local Keycloak for Defender of Egril

This directory contains the local Keycloak IAM setup for development.

## Quick Start (Recommended)

The easiest way to start **all** local services (database, Keycloak, and backend) together is from the **project root**:

```bash
docker compose up -d
```

This single command starts:
- **PostgreSQL** on port `5432`
- **Keycloak** on port `8081` with the `egril` realm and `defender-of-egril` client pre-configured
- **Backend** on port `8080`, connected to the database

## Prerequisites

- [Docker](https://www.docker.com/) and Docker Compose

## Starting Keycloak Only

```bash
cd local-keycloak
docker compose up -d
```

Keycloak will start on **http://localhost:8081** and automatically import the `egril` realm
(including the `defender-of-egril` client) on first startup.

Admin console: http://localhost:8081/admin  
Default credentials: `admin` / `admin`

## Stopping Keycloak

```bash
docker compose down
```

## Realm and Client

The realm `egril` and the client `defender-of-egril` are automatically created when Keycloak
starts, by importing `egril-realm.json`. No manual Terraform steps are needed for local development.

After startup you can register test user accounts in the Keycloak admin console:

1. Go to http://localhost:8081/admin
2. Log in with `admin` / `admin`
3. Select the **egril** realm
4. Go to **Users** → **Add user**
5. Fill in username and click **Save**
6. Go to the **Credentials** tab and set a password

## Terraform (Optional / Production)

The `terraform/` directory contains an optional Terraform script that can be used to configure
a remote Keycloak instance. It creates the same realm and client as the JSON import above.

### Requirements

```bash
cd local-keycloak/terraform
terraform init
```

### Apply

```bash
cd local-keycloak/terraform
terraform apply
```

Type `yes` when prompted. Override `keycloak_url` for a non-local deployment:

```bash
terraform apply -var="keycloak_url=https://your-keycloak.example.com"
```

### Destroy (reset to defaults)

```bash
cd local-keycloak/terraform
terraform destroy
```

## Configuration in the Application

### Frontend (Web/WASM)

The Keycloak URL for the web frontend is configured via a JavaScript variable in `index.html`:

```html
<script>
  window.keycloakConfig = {
    url: 'http://localhost:8081',
    realm: 'egril',
    clientId: 'defender-of-egril'
  };
</script>
```

For a production deployment, set `window.keycloakConfig.url` to your external Keycloak instance before the game scripts load.

### Frontend (Desktop)

The Keycloak URL for the desktop version is read from:
1. Java system property: `-Diam.base.url=https://your-keycloak.example.com`
2. Environment variable: `IAM_BASE_URL=https://your-keycloak.example.com`
3. Default: `http://localhost:8081`

### Backend (Server)

The Keycloak URL for the backend is read from:
1. Environment variable: `IAM_BASE_URL=https://your-keycloak.example.com`
2. Default: `http://localhost:8081`

## Creating Test Users

After applying Terraform:

1. Go to http://localhost:8081/admin
2. Log in with `admin` / `admin`
3. Select the **egril** realm
4. Go to **Users** → **Add user**
5. Fill in username and click **Save**
6. Go to the **Credentials** tab and set a password

Users can then log in to the game using the IAM login button.
