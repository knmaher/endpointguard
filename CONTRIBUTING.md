# Contributing

Use JDK 25 for development when available. JDK 21 is the minimum supported version; keep production code compatible with Java 21.

Run `./mvnw verify` before opening a pull request. CI runs the same command on Java 21 and 25. Include focused tests for behavior changes and describe the problem, solution, and validation in the pull request.

Keep discovery, probing, policy evaluation, and reporting separate. Core should stay independent of Spring, and `endpointguard-annotations` must never gain dependencies, because applications ship it in production. Library modules produce ordinary JARs, not executable Boot applications.

Test actual Spring Security behavior. Do not infer authorization solely from annotations. Never add automatic requests that can trigger unsafe business operations without a safe execution strategy.

Start with a small issue or pull request. Avoid new modules, infrastructure, or future roadmap features until the basic anonymous authorization flow works.
