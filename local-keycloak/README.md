# Local Keycloak for Defender of Egril

This directory contains the local Keycloak IAM setup for development.

## Prerequisites

- [Docker](https://www.docker.com/) and Docker Compose
- [Terraform](https://www.terraform.io/) (for applying realm/client configuration)

## Starting Keycloak

```bash
cd local-keycloak
docker compose up -d
```

Keycloak will start on **http://localhost:8081**.

Admin console: http://localhost:8081/admin  
Default credentials: `admin` / `admin`

## Stopping Keycloak

```bash
docker compose down
```

## Applying Terraform Configuration

The `terraform/` directory contains scripts that create:
- Realm **egril**
- Client **defender-of-egril** (public OIDC client with PKCE support)

### Requirements

Install the Terraform Keycloak provider:

```bash
cd local-keycloak/terraform
terraform init
```

### Apply

Make sure Keycloak is running and healthy, then:

```bash
cd local-keycloak/terraform
terraform apply
```

Type `yes` when prompted.

This creates the realm and client automatically. You can then register user accounts in the Keycloak admin console at http://localhost:8081/admin.

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
