# Easy-Extension框架使用样例
[English](/README.md)

## 样例源码可以参考[easy-extension-sample](https://github.com/xiaoshicae/easy-extension-sample)

## 以SpringBoot项目为例，简单介绍框架用法

### 样例示意图
* 系统扩展点及业务扩展点实现
![](/doc/overview.png)
* 业务A扩展点执行最终效果
![](/doc/biza.png)
* 业务B扩展点执行最终效果
![](/doc/bizb.png)


### 代码实现的效果 (不同业务注入不同扩展点实现)
* 可以直接参考模块: [spring-boot-sample-simple](/spring-boot-sample-simple)
```java
@RestController
@RequestMapping("/api")
public class Controller {
    // 动态注入扩展点1，不同业务会有不同实现
    @ExtensionInject
    private Ext1 ext1;

    // 动态注入扩展点2，不同业务会有不同实现
    @ExtensionInject
    private Ext2 ext2;

    // 动态注入扩展点3，不同业务会有不同实现
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

### 代码实现步骤

#### 1. maven引入
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

#### 2. 定义系统扩展点
```java
// 扩展点1，需要@ExtensionPoint注解，以便spring包扫描识别
@ExtensionPoint
public interface Ext1 {
    String doSomething1();
}

// 扩展点2，需要@ExtensionPoint注解，以便spring包扫描识别
@ExtensionPoint
public interface Ext2 {
    String doSomething2();
}

// 扩展点3，需要@ExtensionPoint注解，以便spring包扫描识别
@ExtensionPoint
public interface Ext3 {
    String doSomething3();
}
```

#### 3. 定义用于业务身份和能力匹配的参数
```java
// 用于业务或者能力匹配的参数
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

#### 4. 为系统提供Default能力实现
```java
// 默认能力，必须实现所有@ExtensionPoint注解标注的扩展点
// 业务没有实现某个扩展点时，默认能力作为兜底实现
// 需要@Component注解，以便spring包扫描识别
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

#### 5. 不同业务方对接口的实现
```java

// 业务A，实现了扩展点1和扩展点2
// 需要@Component注解，以便spring包扫描识别
@Component
public class BusinessA extends AbstractBusiness<MyParam> implements Ext1, Ext2{
    // 业务A身份唯一标识
    @Override
    public String code() {
        return "x.business.a";
    }

    // 命中业务A的生效条件
    @Override
    public Boolean match(MyParam param) {
        return param != null && param.getName().equals("a");
    }

    // 优先级(复杂场景下，业务挂载了能力，可能存在扩展点冲突，需要通过优先级解决冲突)
    // 当前样例先忽略该方法
    @Override
    public Integer priority() {
        return 100;
    }

    // 业务A挂载了哪些能力
    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of();
    }

    // 业务A实现了扩展点1
    @Override
    public String doSomething1() {
        return "businessA doSomething1";
    }

    // 业务A实现了扩展点2
    @Override
    public String doSomething2() {
        return "businessA doSomething2";
    }
}


// 业务B，实现了扩展点1和扩展点3
// 需要@Component注解，以便spring包扫描识别
@Component
public class BusinessB extends AbstractBusiness<MyParam> implements Ext1, Ext3{
    // 业务身B份唯一标识
    @Override
    public String code() {
        return "x.business.b";
    }

    // 命中业务B的生效条件
    @Override
    public Boolean match(MyParam param) {
        return param != null && param.getName().equals("b");
    }

    // 优先级(复杂场景下，业务挂载了能力，可能存在扩展点冲突，需要通过优先级解决冲突)
    // 当前样例先忽略该方法
    @Override
    public Integer priority() {
        return 100;
    }

    // 业务B挂载了哪些能力
    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of();
    }

    // 业务B实现了扩展点1
    @Override
    public String doSomething1() {
        return "businessB doSomething1";
    }

    // 业务B实现了扩展点3
    @Override
    public String doSomething3() {
        return "businessB doSomething3";
    }
}
```


#### 6. interceptor定义(每次请求都需要识别业务身份)
* 定义拦截器
```java

@Component
public class Interceptor implements HandlerInterceptor {

    // 框架启动会自动注入ISessionManager，可以直接通过spring自动注入获取
    @Resource
    private ISessionManager<MyParam> sessionManager;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 根据入参构造用于business和ability匹配的参数
        String name = request.getParameter("name") != null ? request.getParameter("name").trim() : "unknown";

        // 初始化session
        sessionManager.initSession(new MyParam(name));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //  清空session
        sessionManager.removeSession();
    }
}


// 注入Interceptor
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



#### 7. 启动SpringBoot
```java
// 需要@ExtensionScan注解，用来扫描ExtensionPoint注解标识的接口
@SpringBootApplication
@ExtensionScan
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

#### 8. 发起请求进行验证
```http
request:   GET http://localhost:8080/api/process?name=b
response:  ext1 = businessB doSomething1, ext2 = default doSomething2, ext3 = businessB doSomething3

request:   GET http://localhost:8080/api/process?name=b
response:  ext1 = businessA doSomething1, ext2 = businessA doSomething2, ext3 = default doSomething3
```

## 更多
### 复杂场景(业务叠加能力)
* 可以参考模块: [spring-boot-sample-complex](/spring-boot-sample-complex)

### 项目一些后续规划
* 业务支持动态加载
* ......