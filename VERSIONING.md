# Versioning & Naming Convention

## Jar name

```
BetterThanWolves-{mc_version}-{port_version}.jar
```

**Examples**

| Jar name                                      | Meaning                                       |
|-----------------------------------------------|-----------------------------------------------|
| `BetterThanWolves-1.20.1-0.1.0-alpha.1.jar`   | Alpha 1 of port v0.1.0 targeting MC 1.20.1    |
| `BetterThanWolves-1.20.1-0.2.0-beta.3.jar`    | Beta 3 of port v0.2.0                         |
| `BetterThanWolves-1.20.1-1.0.0.jar`           | First stable release                          |
| `BetterThanWolves-1.20.1-1.2.5.jar`           | Patch 5 of minor 2 of stable 1.x              |
| `BetterThanWolves-1.21.0-1.2.5.jar`           | Same port release rebuilt for MC 1.21         |

## Port version — SemVer

The Forge port carries its own SemVer independent of upstream BTW:

```
MAJOR.MINOR.PATCH[-PRERELEASE]
```

| Bump  | When                                                                                   |
|-------|----------------------------------------------------------------------------------------|
| MAJOR | World/save compat breaks, or a public API contract changes                             |
| MINOR | New FC systems bridged, or new modern-MC integrations land                             |
| PATCH | Bug fixes only                                                                         |

**Pre-release tags** (ordered lowest → highest): `alpha.N`, `beta.N`, `rc.N`.

Pre-release versions sort **before** the matching release, i.e.
`0.1.0-alpha.1` < `0.1.0-beta.1` < `0.1.0-rc.1` < `0.1.0` < `0.1.1`.

## Upstream BTW version — frozen

The upstream BTW / FC source version is pinned in `gradle.properties` as
`btwVersion`. It is **not** part of the port SemVer — it only changes
when the BTW source tree is upgraded (rare).

Its current value (`4.B0000003`) is surfaced in three places:

- JAR manifest: `BTW-Upstream-Version: 4.B0000003`
- mods.toml description: *"Runs the original BTW 4.B0000003 code verbatim…"*
- This document and `CURSEFORGE.md`

## MC-version handling

Two MC versions are tracked:

| Property         | Value     | Purpose                                             |
|------------------|-----------|-----------------------------------------------------|
| `mcVersion`      | `1.5.2`   | The vanilla MC the upstream FC code was written for |
| `portMcVersion`  | `1.20.1`  | The vanilla MC this port actually targets           |

Both are surfaced in the JAR manifest as
`BTW-Upstream-MC-Version` and `BTW-Target-MC-Version`.

## Where the version lives at runtime

```
gradle.properties                    ← source of truth
   │   portVersion = 0.1.0-alpha.1
   │   btwVersion  = 4.B0000003
   │
   ├─→ Forge/build.gradle
   │     project.version   → JAR manifest Implementation-Version
   │     archivesName      → jar filename
   │
   └─→ Forge/.../META-INF/mods.toml
         version="${file.jarVersion}"  (Forge resolves from manifest)
         description contains ${btw_version} (Gradle replaces at build)
```

## Release checklist

When cutting a release:

1. Update `portVersion` in `gradle.properties`.
2. Update `CHANGELOG.md` (add entry under the new version).
3. Run `./gradlew :Forge:clean :Forge:build`.
4. Verify jar filename matches expectations.
5. Verify `unzip -p <jar> META-INF/mods.toml` shows the new version.
6. Tag the commit: `git tag -a v0.1.0-alpha.1 -m "Port 0.1.0-alpha.1"`.
7. Upload to CurseForge with display name
   `Better Than Wolves 1.20.1 — 0.1.0-alpha.1 (BTW 4.B0000003)`.
8. Set CurseForge release type (Alpha / Beta / Release) to match the
   pre-release tag.

## Never bump across multiple axes in one commit

Bumping `portVersion`, `btwVersion`, and `portMcVersion` simultaneously
makes it impossible to bisect regressions. When more than one needs to
change, do them as separate commits so the history stays legible.
