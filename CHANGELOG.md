# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/), and the project follows
[Semantic Versioning](https://semver.org/): patch releases never contain source- or binary-incompatible
API changes (enforced by the japicmp gate in `mvn verify`).

## [Unreleased]

> Planned as **3.4.0**. All API changes below are additive (new classes, new `default` methods, new constructors,
> new annotation element with a default value). There are **behavior** changes too: read the next section before
> upgrading. If you would rather ship them with a major version, that is a call for the release (`/release-prep`).

### Behavior changes — read before upgrading

No source or binary break, but these change what an existing application does. The README ("从 3.3 升级") has the
same list with advice on what to check.

- **Exceptions thrown by extension implementations reach the caller unchanged** instead of being wrapped in
  `UndeclaredThrowableException → InvocationTargetException` (up to four levels). Failures to resolve an extension
  (for example no session initialised) surface as `InvokeException` instead of `UndeclaredThrowableException`.
  `-Deasy-extension.legacy-exception-wrapping=true` restores the old behavior for one release (removed in 4.0). It is a
  JVM system property only; the same name in `application.yml` has no effect. A WARN is logged once when it is active.
- **Extension points inherited from a superclass or a super-interface now count.** Only the interfaces a class declares
  itself used to. A business whose superclass implements an extension point now answers for it; the default
  implementation used to. Routing changes without any error: check with `explain(Extension.class)`. An inherited
  extension point that is not registered (its module is not scanned) or not public is ignored, as before; one the class
  declares itself must still be registered.
- **Spring AOP-proxied implementations take part in matching.** `@Cacheable` / `@Transactional` / `@Async` or aspect-matched
  `@Business` / `@Ability` classes used to be skipped silently. With `allow-unknown-business=true` requests that were
  served by the defaults may now reach them, and overlapping businesses make strict mode fail with
  `multiple business found`. Check those classes before upgrading.
- **The framework registers the Spring bean itself** (it used to create a second, private instance). A test double that
  replaces such a bean (`@MockitoBean`, `@MockBean`) is what gets called: stub `match()`.
- **Closing a scoped scope removes only that scope.** `ExtensionSessionScope.openScoped(...).close()` used to remove
  every session of the thread. A default-scope session initialised inside the block now stays; remove it yourself.
  `removeSession()` overrides are no longer called by a scoped close.
- **Error messages changed.** `doRegister()` without any default implementation: `extension point default implementation
  not found, please check instance with @ExtensionPointDefaultImplementation annotation if exist` (was `... should not
  be null`). A default that does not cover an extension point: the message gained a hint about `mandatory = true` and is
  raised once everything is registered. No default at all and no business matching: `SessionException` (was a
  `NullPointerException`).
- **`getInstance()` and `getTargetClass()` are reserved on extension points** (they are answered by the framework's
  proxies, see `IProxy`). Declaring them with another return type fails when the proxy is created.
- **New `default` methods on public interfaces are called by the framework:** `IExtensionRegister#validateRegistration()`,
  `IExtensionSession#removeSession(String)`, the `find…` lookups on the managers. An implementation of those interfaces
  that already has a method with the same signature is now called by it.

### Fixed

- **Spring integration: implementation beans were instantiated twice.** `@Business` / `@Ability` /
  `@ExtensionPointDefaultImplementation` classes now resolve to the *same* singleton the application context
  manages, instead of a second inner-bean copy.
- **Spring integration: implementations wrapped by Spring AOP proxies silently disappeared.** A class that
  carries `@Cacheable` / `@Transactional` / `@Async` or is matched by an aspect (JDK interface proxy or CGLIB
  subclass) used to be skipped without any error and failed later with `no business matched`. The framework now
  reads annotations and extension-point interfaces from the proxy's target class (`IProxy#getTargetClass()`) and
  registers the proxy itself, so advice still applies. `@Ability(requires / excludes)`, `explain()` and the admin
  UI read from the target class as well. Scanned beans that cannot be registered now fail fast at startup with an
  explanatory message.
- A declared checked exception thrown by an extension implementation can be caught as such (see the behavior change
  on exceptions above).
- `ExtensionSessionScope.openScoped` no longer calls the deprecated `initScopedSession`.
- `explain(...)` is derived from the chain bound to the scope. It used to read the most recent resolve trace of the
  thread, which another scope's `initSession` overwrote.
- The annotation processor no longer hard-codes `SourceVersion.RELEASE_21`; it follows the compiler
  (`latestSupported()`), so it loads on JDK 17.

### Changed

- **Build baseline is now JDK 17** (`--release 17`), verified on JDK 17 and 21 against Spring Boot 3.5 and 4.0 in CI.
  Previously the code was compiled for JDK 21 although it does not use any JDK 21 API.
- **Hot path.** The registries read lock-free (immutable copy-on-write snapshots), an extension lookup no longer
  creates an exception per miss, and the resolved chain is immutable and computed once per session.
  Indicative numbers (plain loop, 4 cores, JDK 21, not JMH) against 3.3.6: falling through three misses to a default
  implementation 2.8 µs → 55 ns; a hit at the head of the chain 80 → 32 ns; `initSession` with 1000 businesses
  9.7 → 3.0 µs, with 100 businesses 1.1 → 0.65 µs; four threads now scale (fall-through 1.1 → 65 M ops/s in total).
- **`allow-unknown-business` is shorthand for two independent policies**: `easy-extension.unknown-business-policy`
  (`reject` | `default`: nobody matched) and `easy-extension.multi-match-policy` (`reject` | `select`: several
  matched). When neither is set `allow-unknown-business` decides as before (`false` = both `reject`,
  `true` = `default` + `select`), so existing configurations behave the same.
  With `select`, when nothing the application configured decides between several matching businesses (no
  `business-match-order`, no `BusinessMatchSelector`) the first registered one wins and a WARN is logged once per
  distinct set of matching businesses.
- **Default implementations may be split up.** The starter accepts any number of
  `@ExtensionPointDefaultImplementation` classes / beans, each answering for the extension points it implements
  (it used to require exactly one that implements everything). In a resolved chain they share one "default" place.
  Completeness is checked by `validateRegistration()` once everything is registered (the register helpers and the
  starter call it): every extension point must have a default implementation unless it is
  `@ExtensionPoint(mandatory = true)`. `registerExtensionPointDefaultImplementation` keeps its old single-default
  rules. If two defaults implement the same extension point the error now names the extension point and the classes;
  defaults with different codes must have different priorities (rejected at registration; a chain holds one entry per
  priority, so every session would have failed to resolve). Among several default beans a `@Primary` one alone is the
  default, as before 3.4. A default found by annotation that implements an extension point nobody registered is not held
  against it (a WARN is logged), as before.
- Admin: each extension point shows the source of the default implementation that answers for it; the "default
  implementation" panel shows the first one, and is empty when there is none. The records are unchanged.
- The API-compatibility gate downloads the baseline jar directly from the repository and verifies its SHA-1.
  Resolving it as a dependency compared the module with itself whenever the project version equalled the baseline
  version, which is the case on every branch until the next release bumps it, so the gate passed vacuously.

### Added

- **Hand a request's identity to another thread.** `ResolvedChain` (immutable; its `registryVersion()` fingerprints
  the registry it was resolved against), `IExtensionSession#resolve / currentChain / bind` and
  `ExtensionSessionScope.runWith / restore / restoreScoped`. Binding evaluates no matcher, and refuses a chain from a
  different registry or one that mentions unknown codes. When the task runs on a thread that already has a session
  (direct executor, `CallerRunsPolicy`, parallel streams) that session is bound again afterwards.
- **Interceptors.** `ExtensionInterceptor` / `ExtensionInvocation` and `registerInterceptor(...)` wrap every call to
  an extension implementation made through the context or an `@ExtensionInject` proxy, in registration order
  (`registerInterceptor(null)` throws `RegisterParamException`). The starter registers `ExtensionInterceptor` beans in
  `@Order` order.
- `UnknownBusinessPolicy` and `MultiMatchPolicy`, a `DefaultExtensionContext` constructor taking them, and the two
  Spring properties above.
- `@ExtensionPoint(mandatory = true)`: no default implementation needed; if nothing in the chain implements it the
  error says so. `addExtensionPointDefaultImplementation`, `listExtensionPointDefaultImplementations`,
  `validateRegistration`, `ExtensionContextRegisterHelper#addExtensionPointDefaultImplementations`.
- **Injectable session store**: `DefaultExtensionContext` takes an `IScopedSessionManager`; the starter uses a bean
  of that type if there is exactly one (or one is `@Primary`). The starter also picks up a `BusinessMatchSelector` bean
  on the same terms. With several and no `@Primary` none is used and a WARN says so, instead of failing the startup.
  `IScopedSessionManager` gained `default` methods to bind and read a chain, so existing implementations keep working.
- `removeSession(String scope)`, `registryVersion()`. `ResolveTrace` lists every default implementation in its
  resolution chain (`getDefaultImplCode()` keeps reporting the first); `OrderedCodeBusinessMatchSelector#order()`.
- `IProxy#getTargetClass()`, overloads of `AnnProxyConvertUtils.convertAnn…` and the proxy factories that take the
  target class, and non-throwing `find…` lookups (`default` methods) on `IExtensionPointManager`,
  `IExtensionPointGroupImplementationManager`, `IAbilityManager` and `IBusinessManager`.
- `japicmp` API-compatibility gate in `mvn verify`, GitHub Actions CI (JDK 17/21 × Spring Boot 3.5/4.0), and this
  changelog.
- Tests that start real application contexts (JDK and CGLIB proxies, wiring through properties and beans), and tests
  for exception transparency, the annotation processor, registry snapshots under concurrency, chain hand-off,
  interceptors and the policies.

### Deprecated

- Nothing new. `scopedXxx` / `initScopedSession` keep their `@Deprecated(since = "3.4", forRemoval = true)` marker and
  will be removed in 4.0.

### Notes for maintainers

- The japicmp gate reports the new annotation element `@ExtensionPoint#mandatory()` as "abstract method added".
  That is compatible for every use of the annotation (it has a default value), so it is acknowledged in the root
  `pom.xml` under `<excludes>`. Every future annotation element needs the same explicit acknowledgement.
- After the release, update `easy-extension.baseline.version` in the root `pom.xml` to the released version. The
  downloaded baseline jar carries that version in its file name, so a build without `clean` fetches the new one.
