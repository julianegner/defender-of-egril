# Feedback Email Service Configuration

The backend can send an email notification to an admin address whenever a new
feedback submission arrives. This feature is **optional** — if any of the
required variables are absent the service silently skips the email.

## Environment Variables

All variables are read at runtime from the process environment. Set them in
your deployment environment (e.g. `docker-compose.yml`, Kubernetes secret, or
`.env` file passed to Docker).

| Variable | Required | Example value | Description |
|---|---|---|---|
| `FEEDBACK_SMTP_HOST` | yes | `smtp.example.com` | Hostname of the SMTP relay / mail server |
| `FEEDBACK_SMTP_PORT` | yes | `587` | SMTP port (587 for STARTTLS, 465 for implicit TLS, 25 for plain) |
| `FEEDBACK_SMTP_USERNAME` | yes | `feedback@egril.de` | SMTP authentication username |
| `FEEDBACK_SMTP_PASSWORD` | yes | `s3cr3t` | SMTP authentication password |
| `FEEDBACK_EMAIL_FROM` | yes | `feedback@egril.de` | Sender address that appears in the *From* header |
| `FEEDBACK_EMAIL_TO` | yes | `admin@egril.de` | Recipient address that receives the notification |
| `FEEDBACK_SMTP_STARTTLS` | no | `true` (default) | Set to `false` to disable STARTTLS (e.g. for local test servers) |

> **Note:** If any of the first six variables is missing or empty the email
> service prints an info-level log line and is disabled for that run. No error
> is raised and the feedback submission itself still succeeds.

## Where to Set Them

### docker-compose.yml (local or production)

Add the variables to the `environment` block of the `backend` service:

```yaml
services:
  backend:
    environment:
      # --- existing DB / Keycloak variables ---
      DB_HOST: postgres
      # …

      # --- feedback email (optional) ---
      FEEDBACK_SMTP_HOST: smtp.example.com
      FEEDBACK_SMTP_PORT: "587"
      FEEDBACK_SMTP_USERNAME: feedback@egril.de
      FEEDBACK_SMTP_PASSWORD: s3cr3t
      FEEDBACK_EMAIL_FROM: feedback@egril.de
      FEEDBACK_EMAIL_TO: admin@egril.de
      FEEDBACK_SMTP_STARTTLS: "true"
```

> **Security tip:** Never commit real credentials to version control. Use a
> `.env` file (listed in `.gitignore`) and reference it with
> `env_file: .env` in the service definition, or use Docker / Kubernetes
> secrets and inject them at deploy time.

### .env file (docker compose env_file)

Create a file called `.env` next to `docker-compose.yml` (**not** committed
to the repository):

```
FEEDBACK_SMTP_HOST=smtp.example.com
FEEDBACK_SMTP_PORT=587
FEEDBACK_SMTP_USERNAME=feedback@egril.de
FEEDBACK_SMTP_PASSWORD=s3cr3t
FEEDBACK_EMAIL_FROM=feedback@egril.de
FEEDBACK_EMAIL_TO=admin@egril.de
FEEDBACK_SMTP_STARTTLS=true
```

Then reference it in `docker-compose.yml`:

```yaml
services:
  backend:
    env_file:
      - .env
```

### Kubernetes / Cloud deployments

Create a Kubernetes `Secret` containing the variables and mount it as
environment variables in the backend `Deployment`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: feedback-email-secret
stringData:
  FEEDBACK_SMTP_HOST: smtp.example.com
  FEEDBACK_SMTP_PORT: "587"
  FEEDBACK_SMTP_USERNAME: feedback@egril.de
  FEEDBACK_SMTP_PASSWORD: s3cr3t
  FEEDBACK_EMAIL_FROM: feedback@egril.de
  FEEDBACK_EMAIL_TO: admin@egril.de
  FEEDBACK_SMTP_STARTTLS: "true"
---
# In the Deployment spec:
envFrom:
  - secretRef:
      name: feedback-email-secret
```

## Common SMTP Provider Settings

| Provider | Host | Port | STARTTLS |
|---|---|---|---|
| Gmail (App Password) | `smtp.gmail.com` | `587` | `true` |
| Outlook / Office 365 | `smtp.office365.com` | `587` | `true` |
| Mailgun | `smtp.mailgun.org` | `587` | `true` |
| Postfix (local) | `localhost` | `25` | `false` |
| Mailhog (test) | `localhost` | `1025` | `false` |

## Where the Code Lives

The email service is implemented in:

```
servers/backend/src/main/kotlin/de/egril/defender/FeedbackEmailService.kt
```

It is called from `Routing.kt` after a feedback submission is accepted and
stored in the database.
