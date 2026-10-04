# Security policy

## Supported versions

EndpointGuard has not been released yet. Until 0.1.0 is published, only the latest commit on `main` receives fixes. After that, fixes go into the latest released minor version.

## Reporting a vulnerability

Please report suspected vulnerabilities privately through [GitHub private vulnerability reporting](https://github.com/knmaher/endpointguard/security/advisories/new). Do not open a public issue with exploit details, credentials, or application data.

Include the affected version or commit, reproduction steps, expected behavior, and impact. Response times are best effort while the project is maintained by one person.

## What counts as a vulnerability

EndpointGuard is a test library, so the most serious issue is a **false pass**: an endpoint that accepts anonymous requests but is reported as protected. Please report those as vulnerabilities. Also report anything that makes a scan execute application code it should not, such as a controller method being invoked.

Known limitations listed in the README (for example authorization checks inside handler code, which EndpointGuard does not see) are not vulnerabilities.
