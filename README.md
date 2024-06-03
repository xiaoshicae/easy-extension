# Easy-Extension Frame Sample
[中文](/README.ZH.md)

## sample resource code can refer[easy-extension-sample](https://github.com/xiaoshicae/easy-extension-sample)

## Introducing the Framework with a SpringBoot Project

### Example Illustration
* System Extension Points and Business Extension Points Implementation
  ![](/doc/overview.png)
* Final Effect of Business A Extension Point Execution
  ![](/doc/biza.png)
* Final Effect of Business B Extension Point Execution
  ![](/doc/bizb.png)

### Code Implementation Effects (Different Businesses Inject Different Extension Points)
* Directly refer to the module: [spring-boot-sample-simple](/spring-boot-sample-simple)
```java
@RestController
@RequestMapping("/api")
public class Controller {
    // Dynamically inject Extension Point 1, different businesses will have different implementations
    @ExtensionInject
    private Ext1 ext1;

    // Dynamically inject Extension Point 2, different businesses will have different implementations
    @ExtensionInject
    private Ext2 ext2;

    // Dynamically inject Extension Point 3, different businesses will have different implementations
    @ExtensionInject
    private Ext3 ext3;

    @RequestMapping("/process")
    public String process() {
        String s1 = ext1.doSomething1();
        String s2 = ext2.doSomething2();
        String s3 = ext3.doSomething3();
        return String.format("ext1 = %s, ext2 = %s, ext3 = %s", s1, s2, s3);
    }
}
```

### Code Implementation Steps

#### 1. Maven Introduction
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>io.github.xiaoshicae</groupId>
        <artifactId>easy-extension-spring-boot-starter</artifactId>
        <version>1.0.2</version>
    </dependency>
</dependencies>
```

#### 2. Define System Extension Points
```java
// Extension Point 1, needs @ExtensionPoint annotation for Spring package scanning recognition
@ExtensionPoint
public interface Ext1 {
    String doSomething1();
}

// Extension Point 2, needs @ExtensionPoint annotation for Spring package scanning recognition
@ExtensionPoint
public interface Ext2 {
    String doSomething2();
}

// Extension Point 3, needs @ExtensionPoint annotation for Spring package scanning recognition
@ExtensionPoint
public interface Ext3 {
    String doSomething3();
}
```

#### 3. Define Parameters for Business Identity and Capability Matching
```java
// Parameters for business or capability matching
public class MyParam {
    private final String name;

    public MyParam(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
```

### 4. Provide Default Capability Implementation for the System
```java
// Default capability, must implement all extension points marked with @ExtensionPoint
// Default capability serves as a fallback implementation when a business does not implement a certain extension point
// Needs @Component annotation for Spring package scanning recognition
@Component
public class DefaultAbility extends BaseDefaultAbility<MyParam> implements Ext1, Ext2, Ext3 {
    @Override
    public String doSomething1() {
        return "default doSomething1";
    }

    @Override
    public String doSomething2() {
        return "default doSomething2";
    }

    @Override
    public String doSomething3() {
        return "default doSomething3";
    }
}
```

#### 5. Different Businesses' Implementations of the Interface
```java
// Business A, implements Extension Point 1 and Extension Point 2
// Needs @Component annotation for Spring package scanning recognition
@Component
public class BusinessA extends AbstractBusiness<MyParam> implements Ext1, Ext2 {
    // Unique identity identifier for Business A
    @Override
    public String code() {
        return "x.business.a";
    }

    // Condition for Business A to take effect
    @Override
    public Boolean match(MyParam param) {
        return param != null && param.getName().equals("a");
    }

    // Priority (In complex scenarios, when a business mounts a capability, there may be conflicts in extension points, which need to be resolved through priority)
    // This example ignores this method for now
    @Override
    public Integer priority() {
        return 100;
    }

    // What capabilities does Business A mount
    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of();
    }

    // Business A implements Extension Point 1
    @Override
    public String doSomething1() {
        return "businessA doSomething1";
    }

    // Business A implements Extension Point 2
    @Override
    public String doSomething2() {
        return "businessA doSomething2";
    }
}

// Business B, implements Extension Point 1 and Extension Point 3
// Needs @Component annotation for Spring package scanning recognition
@Component
public class BusinessB extends AbstractBusiness<MyParam> implements Ext1, Ext3 {
    // Unique identity identifier for Business B
    @Override
    public String code() {
        return "x.business.b";
    }

    // Condition for Business B to take effect
    @Override
    public Boolean match(MyParam param) {
        return param != null && param.getName().equals("b");
    }

    // Priority (In complex scenarios, when a business mounts a capability, there may be conflicts in extension points, which need to be resolved through priority)
    // This example ignores this method for now
    @Override
    public Integer priority() {
        return 100;
    }

    // What capabilities does Business B mount
    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of();
    }

    // Business B implements Extension Point 1
    @Override
    public String doSomething1() {
        return "businessB doSomething1";
    }

    // Business B implements Extension Point 3
    @Override
    public String doSomething3() {
        return "businessB doSomething3";
    }
}
```

#### 6. Interceptor Definition (Each request needs to identify the business identity)
* Define an interceptor
```java
@Component
public class Interceptor implements HandlerInterceptor {

    // The framework will automatically inject ISessionManager when it starts, which can be obtained through Spring's automatic injection
    @Resource
    private ISessionManager<MyParam> sessionManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Construct parameters for business and ability matching based on input parameters
        String name = request.getParameter("name") != null ? request.getParameter("name").trim() : "unknown";

        // Initialize session
        sessionManager.initSession(new MyParam(name));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Clear session
        sessionManager.removeSession();
    }
}

// Inject Interceptor
@Configuration
public class WebInterceptorConfigurer implements WebMvcConfigurer {
    @Resource
    private Interceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/**").excludePathPatterns("/favicon.ico");
    }
}
```

#### 7. Start SpringBoot
```java
// Needs @ExtensionScan annotation to scan interfaces marked with @ExtensionPoint
@SpringBootApplication
@ExtensionScan
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

#### 8. Initiate Requests for Verification
```http
request:   GET http://localhost:8080/api/process?name=b
response:  ext1 = businessB doSomething1, ext2 = default doSomething2, ext3 = businessB doSomething3

request:   GET http://localhost:8080/api/process?name=a
response:  ext1 = businessA doSomething1, ext2 = businessA doSomething2, ext3 = default doSomething3
```

## More
### Complex Scenarios (Business Stacking Capabilities)
* Refer to the module: [spring-boot-sample-complex](/spring-boot-sample-complex)

### Some Subsequent Planning for the Project
* Dynamic loading support for businesses
* ......