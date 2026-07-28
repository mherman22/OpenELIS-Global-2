# Release Process

This document describes how OpenELIS Global 2 is versioned and released.

## Versioning

OpenELIS Global 2 uses [Semantic Versioning](https://semver.org/):
`MAJOR.MINOR.PATCH`, with optional pre-release labels (`-beta.N`, `-rc.N`).

The version lives in two places, which MUST always agree (the Release workflow
enforces this):

- `pom.xml` `<version>`
- `frontend/package.json` `version`

Git tags and Docker image tags carry the version **without a `v` prefix** (e.g.
`3.3.0`, not `v3.3.0`) — Docker tags are derived from the git tag verbatim.

### What counts as breaking (MAJOR)

The "public API" that MAJOR protects is the implementer contract:

- Database migrations that require manual intervention or are destructive
- Removed or incompatibly changed REST / FHIR endpoints
- Breaking changes to configuration-package formats
  (`volume/configuration/backend/**`), the plugin API, or module contracts
- Anything that breaks the "pull image, restart, Liquibase runs" upgrade path

### MINOR

Additive changes: new features/modules, new endpoints, additive schema
migrations, new configuration options.

### PATCH

Fixes only. No new features; schema changes absent or trivially additive.

### History note

Releases up to and including the `3.2.1.x` line used a legacy 4-part scheme
(`major.minor.state.fix`, where the third digit was a maturity code: 0=alpha,
1=beta, 2=rc, 3=deployable). The first semver release is `3.3.0`. Numbers that
could be confused with the legacy scheme are never re-issued. Liquibase
changelog directory names (e.g. `liquibase/3.5.x.x/`) are opaque labels and do
NOT track the application version.

## Cutting a release

1. Ensure `develop` is green (Backend, Frontend, and the `03 Checkpoint - E2E`
   status on the target commit).
2. Run the **Release** workflow (`.github/workflows/release.yml`) via _Actions →
   Release → Run workflow_, providing:
   - `version` — the semver to release (e.g. `3.3.0` or `3.4.0-rc.1`)
   - `next_version` — optional next development version (e.g. `3.3.1-SNAPSHOT`);
     leave empty to skip the post-release bump
3. The workflow sets the version in `pom.xml` + `frontend/package.json`, commits
   to `develop`, tags, and publishes a GitHub Release with auto-generated notes.
   Pre-release versions (containing `-`) are marked as GitHub pre-releases
   automatically.

Everything after that is automatic:

- Publishing the release triggers **03 - E2E** on the tagged commit.
- On a green `03 Checkpoint - E2E`, **Publish Images** promotes the exact images
  that passed E2E from GHCR to DockerHub, tagged with the release version
  (backend, frontend, proxy, fhir, database).
- **Packaging / Installer** builds the Linux installer tarball and attaches it
  to the GitHub Release as an asset.

Before publishing, edit the draft/auto-generated release notes to add a curated
**Breaking changes / DB migrations** section when applicable — implementers plan
upgrades around it.

## Hotfixes

1. Branch from the release tag: `git checkout -b release/3.3.x 3.3.0`
2. Land the fix on that branch (PR targeting `release/3.3.x`).
3. Run the Release workflow against that branch with a patch version (`3.3.1`).
4. **Mandatory:** open a PR cherry-picking the fix back to `develop` before the
   hotfix is considered done. A hotfix that only exists on a release branch will
   regress in the next regular release.

Do not add extra version segments (the legacy `3.2.1.8.1` pattern) — a hotfix is
a patch bump.

## Cadence and support

- Time-based: a MINOR release roughly every 6–8 weeks, cut from `develop`.
- PATCH releases as needed for the latest MINOR.
- Use `-rc.N` pre-releases when a release needs field validation before general
  availability.

## Requirements / secrets

- `RELEASE_TOKEN` — a token permitted to push to `develop` (branch protection
  blocks the default `GITHUB_TOKEN`). Used by the Release workflow to push the
  version commit and tag.
- `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` — used by Publish Images (already
  configured for the existing pipeline).
