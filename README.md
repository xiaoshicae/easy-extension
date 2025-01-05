# Easy-Extension

Easy-Extension框架主要解决`复杂系统的扩展性问题`，适用于有多接入方，且不同接入方有定制化的扩展诉求场景。例如电商交易，履约等中台系统。

## ✨ 特性

* 轻量易用，提供springboot-starter方便快速接入
* 提供可视化管理后台，方便查看系统的扩展点，能力及接入的业务方情况
* 可以实现业务逻辑和平台逻辑分离，提高系统扩展性和稳定性

## 🖇️ 架构

![](/doc/global-arc.svg)

## ⭐️ 核心概念

* 扩展点: 扩展点是系统提供的扩展能力，本质就是一个接口，业务方通过自定义实现进行扩展。
* 能力: 能力是一组扩展点的实现，是系统提供的通用产品能力，业务可以通过挂载能力来继承能力的扩展点实现。
* 业务: 业务即系统接入方，可以通过挂载能力来继承能力的扩展点实现，如果能力没有合适的实现，业务也可以对扩展点进行自定义实现。

## 🌈 快速开始

完整样例请参考: [easy-extension-sample](https://github.com/xiaoshicae/easy-extension-sample)

* 第一步: 引入依赖
    ```xml
    <dependency>
        <groupId>io.github.xiaoshicae</groupId>
        <artifactId>easy-extension-spring-boot-starter</artifactId>
        <version>3.0.1</version>
    </dependency>
    ```
* 第二步: 定义扩展点，能力及业务
   ```java
  // 扩展点1
  @ExtensionPoint
  public interface Ext1 {
      String doSomething1();
  }
      
  // 能力X
  @Ability(code = "app.ability.x")
  public class AbilityX implements Ext1 {
      String doSomething1() {
          return "AbilityX doSomething1";
      }
  }
      
  // 业务A 挂载了能力("app.ability.x" 即能力X)
  @Business(code = "biz.groupon.film", abilities = {"app.ability.x"})
  public class BusinessA  implements Ext1 {
      String doSomething1() {
          return "BusinessA doSomething1";
      }
  }
  ```
* 第三步: 注入并使用扩展点
    ```java
    @RestController
    @RequestMapping("/api")
    public class Controller {
        /**
         * 系统提供的扩展点1
         * @ExtensionInject注解 会注入扩展点1的动态代理
         * 运行时会根据匹配到的业务及使用的能力，选择有最高优先级的生效的扩展点实现
         * 如果业务及使用的能力都没有实现该扩展点，则会走默认实现进行兜底
         */
        @ExtensionInject
        private Ext1 ext1;
    
    
        /**
         * 系统提供的扩展点2
         */
        @ExtensionInject
        private Ext2 ext2;
    
        /**
         * 系统提供的扩展点3
         * @ExtensionInject注解 会注入List<Extension>的动态代理，包含所有生效的实现
         * 运行时会根据匹配到的业务及使用的能力，按照优先级依次包含生效的扩展实现
         * List当然也包含扩展点的默认实现
         */
        @ExtensionInject
        private List<Ext3> ext3List;
    
        @RequestMapping("/process")
        public String process() {
            String s1 = ext1.doSomething1(); // 执行扩展点1，具体用哪个实现，由匹配到的业务及生效的能力+优先级决定
            String s2 = ext2.doSomething2(); // 执行扩展点2，具体用哪个实现，由匹配到的业务及生效的能力+优先级决定
    
            List<String> s3List = new ArrayList<>();
            for (Ext3 ext3 : ext3List) {
                s3List.add(ext3.doSomething3()); // 按优先级从高到低，依次执行扩展点3的业务或生效能力的实现
            }
            return String.format("res: ext1 = %s, ext2 = %s, ext3List = %s", s1, s2, Arrays.toString(s3List));
        }
    }
    ```

## 🖥 管理后台
* 提供扩展点，能力和业务的可视化能力
![](/doc/admin-extension.png)
* 提供了扩展点冲突检测能力 (业务及能力可能实现了相同的扩展点，可能存在冲突。发生冲突时，会选择优先级最高的实现)
![](/doc/admin-business-conflict.png)

## 📖 文档

框架设计及详细使用文档请参考: [wiki](https://github.com/xiaoshicae/easy-extension/wiki)

## 🌐 语言

Go版本的easy-extension可以参考: [go-easy-extension](https://github.com/xiaoshicae/go-easy-extension)

## ⚠️ License

Easy-Extension遵循Apache开源协议，具体内容请参考LICENSE文件。
