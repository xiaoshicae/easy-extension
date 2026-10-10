# 绑定指南:框架怎么知道"当前请求属于哪个业务"

> 一句话:**在一次调用开始的地方绑定一次,调用栈里任何地方的扩展点都会读到它。** 绑定属于当前线程。

```
入口(HTTP 请求 / RPC / 消息 / 定时任务 / 测试)
   │  context.bind(param)                       ← 你写,或 starter 替你写(仅 Spring MVC)
   ▼
解析一次:选出业务 + 判断各个能力是否生效   →   不可变的 Resolution 快照
   │  压入"当前线程"的绑定栈
   ▼
你的业务代码 ──► 注入的扩展点 / context.first(..) ──► 读栈顶的 Resolution ──► 调用选中的实现
   │
   ▼  调用结束(close):栈回到上一个绑定
```

## 1. 绑定是什么

- **`bind(param)` = 解析 + 压栈。** 先 `resolve(param)`:选出业务,对它挂载的每个能力调用一次 `match`,得到不可变的 `Resolution`;再把它压进当前线程的绑定栈,返回 `Binding`。
- **读取发生在调用扩展点的那一刻。** `@ExtensionInject` 注入的是代理,每次方法调用都读栈顶的 `Resolution`;没有绑定就抛 `ResolutionException`(`NO_BINDING`),信息里写着该怎么做。
- **可嵌套。** `close()` 回到上一个绑定,所以在一个已绑定的调用里可以临时换一个业务身份。
- **属于线程。** 普通 `ThreadLocal`,**不会传给**新线程、线程池任务、`@Async`、`CompletableFuture`;必须由开启它的线程关闭。虚拟线程各有各的栈。
- **`Resolution` 是快照。** 能力是否生效在 `resolve` 的那一刻就定了;它不可变、线程安全,可以传给别的线程,**不要缓存到下一个请求**。

## 2. 写法速查

| 场景 | 写法 |
|---|---|
| 一段代码要以某个业务运行(脚本、测试、消息、任务) | `context.runWith(param, () -> ...)`,要返回值用 `context.callWith(param, () -> ...)` |
| 代码块会抛受检异常 | `try (Binding b = context.bind(param)) { ... }` |
| **Spring MVC 请求** | 声明一个 `MatcherParamResolver` Bean,starter 自动绑定(见第 3 节) |
| 把绑定带到线程池 | `pool.submit(context.wrap(task))`,或 `context.executor(pool)` |
| Spring 的 `@Async` / `applicationTaskExecutor` | `easy-extension.async-propagation=true` |
| 已有 `Resolution`,要在另一个线程里用 | `context.runWith(resolution, () -> ...)` |
| 弄不清当前有没有绑定 | `context.isBound()`;`context.current()` 的 `toString()` 能直接打印业务和解析链 |

## 3. HTTP(Spring MVC):框架背后做了什么

声明 `MatcherParamResolver` Bean 之后,starter 注册一个全局的 `ExtensionSessionInterceptor`(`HandlerInterceptor`)。对每个到达 Spring MVC 的请求:

1. **`preHandle`**:`MatcherParamResolver` 从请求里构造出匹配参数(例如读请求头),`context` 解析它并把结果绑定到处理线程。
2. **控制器、`@ControllerAdvice`、视图渲染**都在这个绑定下运行,所以注入的扩展点读到的就是本请求的业务。
3. **`afterCompletion`**:关闭绑定。如果处理方法返回了异步结果(`DeferredResult`、`Callable`),在 `afterConcurrentHandlingStarted` 就关闭;异步分发回来时会再次经过 `preHandle`,在新的线程上重新绑定。
4. 一个兜底过滤器(`enable-session-auto-cleanup`,默认开启)在请求结束时清掉线程上残留的绑定,防止线程池复用时泄漏。

**不在覆盖范围内**:Servlet 过滤器(包括 Spring Security 的过滤器链)、你自己开的线程、容器的错误页分发。在这些地方用扩展点,请自己 `runWith`。

**你可以在日志里看到它**:

```
INFO  [Easy Extension] HTTP binding is on: for Spring MVC requests matching [/**] (excluding [/health/**]), the MatcherParamResolver bean derives the param, ...
DEBUG [Easy Extension] bound GET /api/order/checkout to thread [http-nio-8080-exec-1], business [biz.retail]; unbound when the request completes
```

第二行需要 `logging.level.io.github.xiaoshicae.extension.spring.boot.autoconfigure.web=DEBUG`;每次解析的详情(解析链、被跳过的能力、耗时)在 `logging.level.io.github.xiaoshicae.extension.core.internal.Resolver=DEBUG`。没有声明 `MatcherParamResolver` 时,启动日志会说明"HTTP requests are not bound to a business"。

**resolver 返回什么、最少要写什么**:返回业务和能力做 `match` 用的那个类型 `T`。没有内置的默认 resolver(身份放在哪个请求头、哪个参数里只有你知道),但可以很短。身份只是一个业务码时,不需要自己的参数类型,`T` 直接是业务码,每个业务用一行 `match` 认领自己的码:

```java
@Bean MatcherParamResolver<String> resolver() { return request -> request.getHeader("X-Biz-Code"); }

@Business(code = "biz.retail")
public class RetailBusiness implements Matcher<String>, FreightCalcExtension {
    public boolean match(String code) { return "biz.retail".equals(code); }
    ...
}
```

没带请求头时 resolver 返回 `null`,原样交给各业务的 `match`;上面的写法(`"biz.retail".equals(code)`)对 `null` 返回 false,于是没有业务匹配:严格模式下是 `NO_BUSINESS_MATCHED`,设了 `allow-unknown-business=true` 则走默认实现。业务码没有对应的业务时也是一样。能力(`@Ability`)同样实现 `Matcher<T>`,这时它只能看到业务码;能力需要更多信息时,用自己的参数类型,让 resolver 构造它。

**不是每个接口都要走扩展点**:用路径圈定范围。范围外的接口不调用 resolver、不绑定,也不要求带身份;范围内的接口,没匹配到业务在严格模式下就是错误(哪怕这个接口根本没用扩展点),所以不用扩展点的接口(健康检查、普通查询)要排除:

```yaml
easy-extension:
  session-include-path-patterns: [/api/**]               # 只绑定这些路径,缺省全部
  session-exclude-path-patterns: [/api/ping, /actuator/**] # 这些路径不绑定(先包含,再排除)
```

接口都在一个前缀下时用 `include`,零散的几个普通接口用 `exclude`。

**没匹配到业务会怎样**:严格模式(缺省)下 `preHandle` 抛 `ResolutionException`(`NO_BUSINESS_MATCHED` 等),按 Spring 的常规异常处理,缺省是 500。想返回 4xx,写一个 `@ExceptionHandler`:

```java
@RestControllerAdvice
class ExtensionErrors {
    @ExceptionHandler(ResolutionException.class)
    ResponseEntity<String> handle(ResolutionException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(e.getMessage());
    }
}
```

**各个接口的参数不一样怎么办**

框架不看控制器方法的参数:绑定发生在 `preHandle`,那时请求体还没被反序列化,控制器的参数还不存在。`MatcherParamResolver` 拿到的只有 `HttpServletRequest`,一个 Bean 处理所有接口,产出同一种类型 `T`。所以"接口参数不同"只意味着一件事:resolver 要能从各种请求里读出**业务身份**。

| 业务身份在哪里 | 做法 |
|---|---|
| 每个请求都带:请求头、令牌里的租户/渠道、登录用户 | 一个 resolver 读它,和接口无关(推荐) |
| 位置因接口而异:这个在路径里,那个在查询参数里 | resolver 按匹配到的路径模板分支(见下) |
| 只在 JSON 请求体里 | 不要在 resolver 里读流:把该路径加入 `session-exclude-path-patterns`,控制器反序列化之后 `context.callWith(param, () -> ...)` |
| 各组接口的读法完全不同 | 不声明 Bean,按路径组各注册一个 `ExtensionSessionInterceptor`(见下一段) |

`preHandle` 时 resolver 能读到:请求头、查询参数和表单参数(`getParameter`)、Servlet 过滤器放进请求或线程里的东西(过滤器链先于拦截器运行,例如登录用户),以及 Spring MVC 已经算好的路径信息:

```java
@Bean
@SuppressWarnings("unchecked")
MatcherParamResolver<OrderParam> resolver() {
    return request -> {
        String pattern = String.valueOf(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));  // 如 /orders/{tenant}/pay
        var pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return switch (pattern) {
            case "/orders/{tenant}/pay" -> new OrderParam(pathVariables.get("tenant"));
            case "/search"              -> new OrderParam(request.getParameter("biz"));
            default                     -> new OrderParam(request.getHeader("X-Tenant"));
        };
    };
}
```

`HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE` 是匹配到的 `HandlerMethod`,想按自己的注解区分接口时可以读它。两点限制:

- **不要在 resolver 里读请求体**(`getInputStream()` / `getReader()`)。Servlet 的请求体只能读一次:读过之后,控制器的 `@RequestBody` 拿到空体(400,`Required request body is missing`);先 `getReader()` 的话还会 500(`getReader() has already been called`)。`getParameter` 是安全的,JSON 请求体不受影响。
- **只能有一个 `MatcherParamResolver` Bean**,声明两个启动就失败(`NoUniqueBeanDefinitionException`);一个 `ExtensionContext<T>` 也只有一种 `T`。各个接口的身份信息要装进同一个类型里(例如 `BizParam(bizCode, scene, attributes)`),而不是每个接口一种参数类型。

**自己注册拦截器**(想完全看得见、控制顺序、或者各组接口的读法不同时):不声明 `MatcherParamResolver` Bean,自己写和 starter 一样的东西。可以按路径组注册多个,每个只管自己的路径,没被任何拦截器覆盖的路径不绑定:

```java
@Configuration
class WebConfig implements WebMvcConfigurer {
    private final ExtensionContext<OrderParam> context;

    WebConfig(ExtensionContext<OrderParam> context) { this.context = context; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ExtensionSessionInterceptor<>(context, request -> OrderParam.from(request)))
                .addPathPatterns("/api/**");
        registry.addInterceptor(new ExtensionSessionInterceptor<>(context, request -> new OrderParam(request.getHeader("X-Tenant"))))
                .addPathPatterns("/partner/**");
    }
}
```

## 4. 其他入口

starter 只认识 Spring MVC。其他入口都是同一个模式:**从入口收到的数据构造 `param`,在真正执行业务的线程上 `runWith`。**

**消息消费者**

```java
@KafkaListener(topics = "orders")
public void onOrder(OrderMessage msg) {
    context.runWith(new OrderParam(msg.getBizCode()), () -> orderService.handle(msg));
}
```

**定时任务**

```java
@Scheduled(cron = "0 0 2 * * *")
public void settle() {
    for (String bizCode : bizCodes) {
        context.runWith(new OrderParam(bizCode), () -> settlement.run());
    }
}
```

**测试**

```java
@Test
void retailPaysFreight() {
    context.runWith(new OrderParam("retail"), () -> assertEquals(new BigDecimal("8.00"), freight.calcFreight(ctx)));
}
```

**RPC**:跨进程传的是**业务身份**(`param` 的字段),不是 `Resolution`(它是进程内快照,不可序列化)。调用方把业务码等字段放进 metadata / attachment / 请求头,被调用方在服务端入口用它构造 `param`。想让下游沿用同一个业务,转发 `context.current().trace().matchedBusinessCode()`,下游的业务用 `match` 认领这个 code。

gRPC 的要点是:业务方法不一定跑在 `interceptCall` 的线程上,所以在监听器的回调里绑定(示意代码,未针对具体版本编译):

```java
public <Q, R> ServerCall.Listener<Q> interceptCall(ServerCall<Q, R> call, Metadata headers, ServerCallHandler<Q, R> next) {
    Resolution resolution;
    try {
        resolution = context.resolve(new OrderParam(headers.get(BIZ_CODE)));       // 解析一次
    } catch (ResolutionException e) {                                               // 严格模式下没有匹配的业务
        call.close(Status.FAILED_PRECONDITION.withDescription(e.getMessage()), new Metadata());
        return new ServerCall.Listener<>() {};
    }
    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(next.startCall(call, headers)) {
        @Override public void onMessage(Q message) { context.runWith(resolution, () -> super.onMessage(message)); }
        @Override public void onHalfClose()        { context.runWith(resolution, () -> super.onHalfClose()); }
        // onReady / onCancel / onComplete 同理
    };
}
```

Dubbo 在提供者侧的 `Filter` 里用 `invocation.getAttachment("bizCode")` 构造 `param`,把 `invoker.invoke(invocation)` 包进 `runWith`/`callWith`(`Filter` 由 Dubbo SPI 创建,不是 Spring Bean,取 `ExtensionContext` 需要走 Dubbo 的 Spring 注入或静态持有者)。

**响应式(WebFlux)**:ThreadLocal 不适用。显式传递 `Resolution`,在用到扩展点的那一步 `context.callWith(resolution, ...)`。

## 5. 换线程

绑定不会自己跟过去,有四种交接方式:

```java
pool.submit(context.wrap(() -> notifier.send()));                       // 包一个任务
CompletableFuture.supplyAsync(() -> compute(), context.executor(pool)); // 包一个线程池,每个任务沿用提交者的绑定
Resolution r = context.current();                                       // 手动:取出来,在目标线程里 runWith
pool.submit(() -> context.runWith(r, () -> compute()));
```

Spring 管理的线程池:设置 `easy-extension.async-propagation=true`,`@Async` 和 `applicationTaskExecutor` 就会沿用提交者的绑定(它是一个 `TaskDecorator` Bean,Spring Boot 会把它和你自己的 `TaskDecorator` 组合在一起)。自己创建的 `ThreadPoolTaskExecutor` 用 `executor.setTaskDecorator(new ExtensionTaskDecorator(context))`。

要点:
- **在提交的那一刻捕获**,不是在任务运行时。任务看到的是那一刻的快照,哪怕原来的请求已经结束。
- 提交时线程上**没有绑定**,任务就原样运行(不绑定),之后用扩展点会得到 `NO_BINDING`。
- 池线程上的绑定在任务结束时一定被清掉,不会漏给下一个任务。
- `CompletableFuture` 的后续阶段(`thenApplyAsync(fn, executor)`)通常由完成上一阶段的线程提交,捕获的是**那个线程**当时的绑定。链路较长时,用 `Resolution` 显式传递更清楚。

## 6. 出问题时怎么查

`ResolutionException: no resolution is bound to thread [...]`(`NO_BINDING`)——当前线程上没有绑定。逐条检查:

1. 这条调用的入口是什么?Spring MVC 请求会自动绑定(前提是声明了 `MatcherParamResolver` Bean,看启动日志有没有 `HTTP binding is on`);其他入口要自己 `runWith`。
2. 是不是在 Servlet 过滤器、拦截器 `preHandle` 之前、或你自己的线程/线程池里?换线程要交接(第 5 节)。
3. 路径是否被 `session-exclude-path-patterns` 排除,或不在 `session-include-path-patterns` 里?
4. `context.isBound()` 能在可疑位置直接回答"这里有没有绑定";`context.current()` 可以直接打印(`Resolution[business=..., chain=[...]]`)。
5. 打开 `Resolver` 和 web 包的 DEBUG 日志(第 3 节),看每个请求绑定了谁。
6. 想知道"为什么选了这个实现":`context.current().explain(MyExtension.class)`。

`ResolutionException: no business matched`(`NO_BUSINESS_MATCHED`)——绑定时没有业务匹配。严格模式的预期行为;放行未知业务设 `easy-extension.allow-unknown-business=true`,此时只走默认实现。
