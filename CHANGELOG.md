# Changelog

## 4.1.0 (unreleased)

### Added
- `ExtensionContext.runWith` / `callWith` (for a param or an existing `Resolution`): bind, run and unbind in one call, for any entry point (RPC, messages, jobs, tests).
- `ExtensionContext.isBound()`, `wrap(Runnable)` and `executor(Executor)`: hand the binding of the calling thread over to the thread that runs a task.
- Starter: `ExtensionTaskDecorator`, and `easy-extension.async-propagation=true` to apply it to the task executors Spring Boot configures (`@Async`, `applicationTaskExecutor`); it is combined with the application's own `TaskDecorator` beans.
- Starter: `easy-extension.session-include-path-patterns` (the existing `session-exclude-path-patterns` applies after it).
- HTTP binding is visible: an INFO line at startup says what is bound for which paths (or that nothing binds requests because there is no `MatcherParamResolver` bean), and a DEBUG line per request says what was bound.
- The built-in `Resolution` prints the business and the resolution chain in `toString()`, so `context.current()` can be logged.
- [doc/binding.md](doc/binding.md): how binding works, every entry point, switching threads, troubleshooting.

### Changed
- The `NO_BINDING` message says what to do (bind where the call starts, hand the binding over on other threads, the Spring MVC support of the starter). The old tail `, bind one first: try (Binding b = context.bind(param)) { ... }` is gone: match on `ResolutionException#reason() == NO_BINDING`, not on the text.
- Servlet applications log one more INFO line at startup. Without a `MatcherParamResolver` bean they also get a small bean (`extensionHttpBindingHint`) that only logs that requests are not bound.

### Fixed
- Admin UI: the bundle embedded in `easy-extension-admin-spring-boot-starter` is rebuilt from the current frontend sources. 4.0.0 shipped the pre-4.0 bundle, so the business conflict table still said "priority" instead of the `abilities` order. The English locale also had an unescaped apostrophe that broke the frontend build.

### Compatibility notes
- Binary compatible with 4.0.0. The new `ExtensionContext` methods are `default` methods.
- Source: a class or interface that implements/extends `ExtensionContext<Resolution>` (an unusual type argument) no longer compiles, because `runWith`/`callWith` become ambiguous; override `runWith(Resolution, Runnable)` and `callWith(Resolution, Supplier)`. Any implementation that already declares a method with one of the new names but another return type fails the same way.
- Mockito mocks of `ExtensionContext` also stub the new defaults: `runWith` does not run the body, `callWith`/`wrap`/`executor` return `null`, `isBound` is `false`. Tests that use them should build a real context (`ExtensionContext.builder()`) or use `CALLS_REAL_METHODS`.
- A custom `ExtensionContext` implementation inherits `isBound()`, which relies on `current()` throwing `ResolutionException` with reason `NO_BINDING` when nothing is bound.

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
