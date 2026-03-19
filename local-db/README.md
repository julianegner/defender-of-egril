# Local Database for Defender of Egril

This folder contains the Docker Compose configuration to run a local PostgreSQL database for development and testing of the Defender of Egril backend.

## Prerequisites

- [Docker](https://www.docker.com/get-started) installed and running
- [Docker Compose](https://docs.docker.com/compose/install/) (included with Docker Desktop)

## Starting the Database

From this directory, run:

```bash
docker compose up -d
```

The database will be available at `localhost:5432` once the health check passes (usually within a few seconds).

## Stopping the Database

```bash
docker compose down
```

To stop the database **and remove all stored data**:

```bash
docker compose down -v
```

## Connection Details

| Property | Value            |
|----------|------------------|
| Host     | `localhost`      |
| Port     | `5432`           |
| Database | `defenderofegril`|
| User     | `defender`       |
| Password | `defender`       |

## Running the Backend with the Database

The backend reads its database configuration from environment variables (with the local defaults already matching the Docker setup above). To start the backend server:

```bash
cd ..
./gradlew :server:run
```

To override the database connection, set any of the following environment variables before running:

| Variable      | Default          | Description          |
|---------------|------------------|----------------------|
| `DB_HOST`     | `localhost`      | PostgreSQL host      |
| `DB_PORT`     | `5432`           | PostgreSQL port      |
| `DB_NAME`     | `defenderofegril`| Database name        |
| `DB_USER`     | `defender`       | Database user        |
| `DB_PASSWORD` | `defender`       | Database password    |
| `DB_ENABLED`  | `true`           | Enable DB connection |

## Database Schema

The database schema is managed by [Liquibase](https://www.liquibase.org/) and is applied automatically on backend startup. Migration scripts are located in:

```
server/src/main/resources/db/changelog/
```

## Verifying Migrations

After starting the backend, you can verify that Liquibase ran the migrations successfully by querying the tracking table:

```bash
docker exec defender-of-egril-db psql -U defender -d defenderofegril \
  -c "SELECT id, filename, dateexecuted, exectype FROM databasechangelog ORDER BY orderexecuted;"
```

You can also check that the `events` table exists:

```bash
docker exec defender-of-egril-db psql -U defender -d defenderofegril \
  -c "\d events"
```

## Troubleshooting

**Container fails to start**: Check that port 5432 is not already in use on your machine (`lsof -i :5432` on Linux/macOS).

**Backend cannot connect**: Ensure the container is healthy (`docker compose ps`) and the connection details match the environment variables (or defaults) configured for the backend.

**Migration failure**: Check the backend logs for Liquibase error messages. You can inspect the `databasechangelog` and `databasechangeloglock` tables in the database for migration state. If a changeset failed mid-run, you may need to manually remove the lock: `DELETE FROM databasechangeloglock;`
