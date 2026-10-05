# Changelog

## 4.0.0 (unreleased)

**BREAKING CHANGE:** 4.0 is a clean redesign and is not source- or binary-compatible with 3.x. See [doc/migration-4.0.md](doc/migration-4.0.md).

### Added
- `ExtensionContext<T>` (immutable, builder based), `Resolution` (immutable snapshot), `Binding` (nestable thread binding).
- `@DefaultImplementation` per extension point; one class may back several points. Extension points whose methods all return `void` get a framework-provided no-op default; a lambda `@Bean` (or `builder.defaultImplementationFor`) can serve as default for a single-method extension point. Hence there is no `optional` flag: every extension point always resolves.
- `@Business(uses = {...})` with `Self.class`; array order is the priority.
- Extension points derived from the full type hierarchy; build-time validation of the whole assembly.
- `BusinessResolver` / `BusinessSelector` SPIs; `ExtensionCatalog` read-only metadata.
- Starter: zero-config scanning, `MatcherParamResolver` + automatic request binding (async aware), `matcher-param-type` property.
- ArchUnit rules keeping core Spring-free and `core.internal` private.

### Fixed (since the first 4.0 draft)
- A business/ability/default that is a JDK dynamic proxy (`spring.aop.proxy-target-class=false`) is accepted.
- `builder.defaultImplementationFor(point, impl)` only covers the given extension point.
- `Binding.close()` is idempotent from any thread; the container's error dispatch is never bound; `easy-extension.session-exclude-path-patterns` keeps health checks etc. out of strict-mode matching.

### Changed
- `ExtensionContext.proxy/proxyAll` throw `ResolutionException(EXTENSION_NOT_FOUND)` (not `IllegalArgumentException`) for an unregistered extension point.
- `ExtensionProxies` lives in `core.internal`; use `context.proxy(...)` / `context.proxyAll(...)`.
- Starter bean names of the per-extension-point infrastructure beans use the fully qualified interface name; do not refer to them by name.
- `ExtensionContext` is built after all singletons are ready: using `@ExtensionInject` inside abilities/businesses no longer causes a circular dependency.
- All exceptions are unchecked (`RegistrationException`, `ResolutionException`).
- Annotation processor metadata.json is version 2.0 (`uses`, `DefaultImplementation`).
- Admin reads `ExtensionCatalog`; `priority` is the position in `uses`; `/default-implementation` returns a list.
- IntelliJ plugin understands `uses` / `Self`.

### Removed
- `IExtensionContext`, `IExtensionReader`, `IExtensionRegister`, `IAbility`, `IBusiness`, `IProxy`, numeric priorities and the `"code::n"` DSL.
- Named scopes (`initScopedSession`) and runtime (dynamic) registration.
- `@ExtensionPointDefaultImplementation`, `easy-extension.enable-log`.
