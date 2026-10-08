# Changelog

## 4.0.0 (2026-10-08)

**BREAKING CHANGE:** 4.0 is a clean redesign and is not source- or binary-compatible with 3.x. See [doc/migration-4.0.md](doc/migration-4.0.md).

### Added
- `ExtensionContext<T>` (immutable, builder based), `Resolution` (immutable snapshot), `Binding` (nestable thread binding).
- `@DefaultImplementation` per extension point; one class may back several points. Extension points whose methods all return `void` get a framework-provided no-op default; a lambda `@Bean` (or `builder.defaultImplementationFor`) can serve as default for a single-method extension point. Hence there is no `optional` flag: every extension point always resolves.
- `@Business(abilities = {...})` with `Self.class`; array order is the priority (`Self.class` marks where the business itself ranks).
- Extension points derived from the full type hierarchy; build-time validation of the whole assembly.
- `BusinessResolver` / `BusinessSelector` SPIs; `ExtensionCatalog` read-only metadata.
- Starter: zero-config scanning, `MatcherParamResolver` + automatic request binding (async aware), `matcher-param-type` property.
- ArchUnit rules keeping core Spring-free and `core.internal` private.

### Fixed (since the first 4.0 draft)
- A business/ability/default that is a JDK dynamic proxy (`spring.aop.proxy-target-class=false`) is accepted.
- `builder.defaultImplementationFor(point, impl)` only covers the given extension point.
- A business may implement no extension point (it only identifies the request and falls through to the defaults).
- Admin `GlobalExceptionHandler` is scoped to the admin API: it used to rewrite the errors of every controller of the host application.
- `Binding.close()` is idempotent from any thread; the container's error dispatch is never bound; `easy-extension.session-exclude-path-patterns` keeps health checks etc. out of strict-mode matching.

### Changed
- `ExtensionContext.proxy/proxyAll` throw `ResolutionException(EXTENSION_NOT_FOUND)` (not `IllegalArgumentException`) for an unregistered extension point.
- `ExtensionProxies` lives in `core.internal`; use `context.proxy(...)` / `context.proxyAll(...)`.
- Starter bean names of the per-extension-point infrastructure beans use the fully qualified interface name; do not refer to them by name.
- `ExtensionContext` is built after all singletons are ready: using `@ExtensionInject` inside abilities/businesses no longer causes a circular dependency.
- All exceptions are unchecked (`RegistrationException`, `ResolutionException`).
- Annotation processor metadata.json is version 2.0 (`abilities`, `DefaultImplementation`).
- Admin reads `ExtensionCatalog`; `priority` is the position in `abilities`; `/default-implementation` returns a list.
- IntelliJ plugin understands `abilities` / `Self`.

### Removed
- `IExtensionContext`, `IExtensionReader`, `IExtensionRegister`, `IAbility`, `IBusiness`, `IProxy`, numeric priorities and the `"code::n"` DSL.
- Named scopes (`initScopedSession`) and runtime (dynamic) registration.
- `@ExtensionPointDefaultImplementation`, `@MatcherParam`, `easy-extension.enable-log` (use the `Resolver` logger at DEBUG).
- Java 17 support: 4.0 needs JDK 21.
