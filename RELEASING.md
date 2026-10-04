# Releasing

EndpointGuard is published to Maven Central under `io.github.knmaher`. Releases on Maven Central are permanent: a published version can never be changed or deleted, so a broken release is fixed by publishing the next patch version.

## One-time setup

1. **Sonatype Central account.** Sign in at [central.sonatype.com](https://central.sonatype.com) with the `knmaher` GitHub account. The `io.github.knmaher` namespace is verified automatically for GitHub sign-ins; check it under *Namespaces*.
2. **Publishing token.** In Central, open *Account → Generate User Token*. Add the two values as GitHub repository secrets (*Settings → Secrets and variables → Actions*):
   - `MAVEN_CENTRAL_USERNAME`
   - `MAVEN_CENTRAL_PASSWORD`
3. **Signing key.** Create a key and publish its public half so Central can verify signatures:

   ```sh
   gpg --quick-gen-key "Maher Kassem Naser <you@example.com>" rsa4096 sign 2y
   gpg --list-secret-keys --keyid-format long          # note the key ID after "rsa4096/"
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --armor --export-secret-keys <KEY_ID>           # copy the whole output
   ```

   Add the exported private key and its passphrase as repository secrets:
   - `MAVEN_GPG_KEY`
   - `MAVEN_GPG_PASSPHRASE`

## Releasing a version

Example for `0.1.0`.

1. **Prepare the release** on a branch and merge it through a pull request:

   ```sh
   ./mvnw versions:set -DnewVersion=0.1.0 -DgenerateBackupPoms=false -DprocessAllModules=true
   ```

   - In `CHANGELOG.md`, rename *Unreleased* to `0.1.0 - YYYY-MM-DD`.
   - In `README.md`, replace `0.1.0-SNAPSHOT` with `0.1.0` and remove the pre-release note.

2. **Tag the merged commit** on `main`:

   ```sh
   git checkout main && git pull
   git tag -a v0.1.0 -m "EndpointGuard 0.1.0"
   git push origin v0.1.0
   ```

   The *Release* workflow checks that the tag matches the project version, builds and tests with Java 21, signs every artifact, and uploads the deployment to Central. The sample module is not published.

3. **Review and publish.** On [central.sonatype.com](https://central.sonatype.com) open *Deployments*, check that the deployment validated and contains `endpointguard-parent`, `endpointguard-annotations`, `endpointguard-core`, and `endpointguard-spring-test`, then click *Publish*. This is the point of no return. Artifacts usually appear on Maven Central within 30 minutes.

4. **Create the GitHub release** from the `v0.1.0` tag, with the changelog entries as release notes.

5. **Start the next version** through another pull request:

   ```sh
   ./mvnw versions:set -DnewVersion=0.2.0-SNAPSHOT -DgenerateBackupPoms=false -DprocessAllModules=true
   ```

   Add a new *Unreleased* section to `CHANGELOG.md`.

## Rehearsing locally

To check signing and the generated jars without uploading:

```sh
export MAVEN_GPG_KEY="$(gpg --armor --export-secret-keys <KEY_ID>)"
export MAVEN_GPG_PASSPHRASE=...
./mvnw -Prelease verify
```

Each published module's `target/` directory then contains the jar, sources jar, Javadoc jar, POM, and a `.asc` signature for each.
