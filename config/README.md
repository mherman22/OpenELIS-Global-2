# OpenELIS Global Configuration (`config/`)

This directory contains **operator-managed configuration and overrides** that
vary between deployments (development, staging, production).

## Philosophy

**Code belongs in Git. Configuration varies by environment.**

This directory contains **only** things that genuinely differ between
deployments:

- **Secrets** (credentials, API keys, certificates) — never committed
- **Server configuration** (nginx.conf, oe_server.xml) — can be
  version-controlled, but may vary per environment
- **Optional overrides** — operators can override default configurations if
  needed

Everything else ships inside the Docker image or is provided via environment
variables.

---

## Directory Structure

### `config/secrets/` — Sensitive Information ⚠️

**Contains:** Database passwords, API credentials, OAuth tokens, certificate
passwords.

**Files:**

- `common.properties` — **NEVER COMMIT THIS**. Contains runtime secrets and
  configuration.
  - Copy from `common.properties.example` and fill in your actual values
  - Referenced in `docker-compose.yml` as a Docker secret:
    `./config/secrets/common.properties:/run/secrets/common.properties`
  - Contains SSL keystore paths, FHIR endpoints, Odoo credentials, facility FHIR
    server details

**Security:**

- This entire directory should be `.gitignore`d
- Only `*.example` files are tracked in Git (templates for operators)
- In production, use Docker secrets or a secrets vault instead of filesystem
  files

---

### `config/overrides/` — Optional Configuration Overrides

**Contains:** Files that operators can customize to override application
defaults.

**When to use:** Only if you need to customize the built-in defaults.

**Files (optional):**

- `SystemConfiguration.properties` — System property overrides (usually not
  needed; defaults in classpath)
- `analyzer-test-map.csv` — Analyzer test name mappings (optional override)
- `odoo-test-product-mapping.csv` — Odoo integration test mappings (optional
  override)
- `menu_config.json` — Menu filtering rules (optional override)

**Note:** These are **optional**. If not present, the application uses defaults
shipped in the WAR.

---

### `config/nginx/` — NGINX Proxy Configuration

**Contains:** NGINX configuration files for the reverse proxy.

**Files:**

- `nginx-dev.conf` — Development proxy configuration (HTTP only)
- `nginx-prod.conf` — Production proxy configuration (HTTPS with SSL)

**Note:** Referenced in `docker-compose.yml`:

```yaml
proxy:
  volumes:
    - ./config/nginx/nginx-prod.conf:/etc/nginx/nginx.conf:ro
```

---

## Usage

### Development Setup

```bash
# 1. Create local configuration (copy from example)
cp config/secrets/common.properties.example config/secrets/common.properties

# 2. Edit config/secrets/common.properties with your local values
# Set DB passwords, SSL paths, etc.

# 3. Start containers
docker compose up -d
```

### Production Deployment

In production, do **NOT** mount configuration files from the filesystem.
Instead:

1. Use Docker secrets for `common.properties` (credentials managed by
   orchestration platform)
2. Use environment variables for environment-specific values
3. Bake server configurations into Docker images during build

See the installer template:
`install/installerTemplate/linux/templates/docker-compose.yml`

---

## What's NOT Here

The following are **NOT** in `config/` because they ship with the application:

- **Domain configurations** (dictionaries, roles, templates) →
  `src/main/resources/configuration/backend/` (in the WAR)
- **Application property defaults** →
  `src/main/resources/application.properties` (in the WAR)
- **Liquibase migrations** → `db/` (in the WAR)
- **Web application files** → `frontend/`, `src/main/webapp/` (in the WAR)

---

## Configuration Loading Order

When the application starts, it loads configuration in this priority (highest
wins):

1. **System property defaults** (hardcoded in Java)
2. **Classpath defaults** (`application.properties`,
   `SystemConfiguration.properties` in WAR)
3. **Spring profile properties** (`application-{profile}.properties` in WAR)
4. **Docker secrets** (`/run/secrets/common.properties` →
   `file:/run/secrets/common.properties`)
5. **Operator filesystem overrides**
   (`/var/lib/openelis-global/properties/SystemConfiguration.properties`
   optional mount)
6. **Database configuration** (`site_information` table — highest priority)

This means:

- Operators can override anything via filesystem mount (but this is optional)
- Secrets vault values override classpath defaults
- Database settings override everything except shell environment variables

---

## .gitignore Rules

```gitignore
# Secrets directory (never commit credentials)
config/secrets/common.properties
config/secrets/datasource.password

# Keep templates for documentation
!config/secrets/*.example
```

Examples (`.example` files) are tracked in Git so operators know what to fill
in.

---

## Related Files

- **Docker Compose:** `docker-compose.yml` (references this directory's files as
  volumes/secrets)
- **Application Properties:** `src/main/resources/application.properties` (base
  defaults)
- **Spring Profiles:**
  `src/main/resources/application-{local,production}.properties`
  (environment-specific defaults)
- **Java Configuration:**
  `src/main/java/org/openelisglobal/config/AppConfig.java` (property source
  loading)
- **System Configuration Singleton:**
  `src/main/java/org/openelisglobal/common/util/DefaultConfigurationProperties.java`
  (merges all sources)

---

## Summary

| File Type          | Location                            | Version Controlled      | When to Use             |
| ------------------ | ----------------------------------- | ----------------------- | ----------------------- |
| Secrets            | `config/secrets/*.properties`       | ❌ No (only `.example`) | Credentials, API keys   |
| Optional Overrides | `config/overrides/*`                | ✅ Yes (as needed)      | Customize defaults      |
| Server Config      | `config/nginx/*`                    | ✅ Yes                  | Reverse proxy setup     |
| App Defaults       | `src/main/resources/`               | ✅ Yes                  | Code-as-config          |
| Domain Configs     | `src/main/resources/configuration/` | ✅ Yes                  | Dictionaries, templates |

**Rule of thumb:** If it's an operator-specific secret or environment-specific
override, it goes in `config/`. Everything else is in the codebase.
