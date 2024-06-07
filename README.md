# Easy-Extension
Easy-Extension框架目标是提高`复杂系统的扩展性`，适用于系统有多个接入方，且不同接入方有定制化的扩展诉求。例如电商交易，履约等中台系统。

# 框架特点
* 轻量易用
* 可以实现业务逻辑和平台逻辑分离，提高提供扩展性和稳定性

# 框架解决的业务场景
![](/doc/target.png)

# 框架使用Demo
```java
@RestController
@RequestMapping("/api")
public class Controller {
    // 动态注入扩展点1，不同业务和能力会有不同实现
    // 会根据匹配到的业务和挂载的扩展点，注入优先级最高的实现
    // 未匹配到任何业务和能力的实现，会走默认能力进行兜底
    @ExtensionInject
    private Ext1 ext1;

    // 动态注入扩展点2，不同业务会和能力有不同实现
    // 会根据匹配到的业务和挂载的扩展点，注入优先级最高的实现
    // 未匹配到任何业务和能力的实现，会走默认能力进行兜底
    @ExtensionInject
    private Ext2 ext2;

    // 动态注入扩展点3，不同业务会和能力有不同实现
    // 会根据匹配到的业务和挂载的扩展点，注入所有匹配到的实现
    // 包括默认能力
    @ExtensionInject
    private List<Ext3> ext3List;

    @RequestMapping("/process")
    public String process() {
        String s1 = ext1.doSomething1();
        String s2 = ext2.doSomething2();
        List<String> s3List = new ArrayList<>();
        for (Ext3 ext3 : ext3List) {
            s3List.add(ext3.doSomething3());
        }
        return String.format("res: ext1 = %s, ext2 = %s, ext3List = %s", s1, s2, Arrays.toString(s3List));
    }
}
```

# 文档
* 完整文档请参考: [wiki](https://github.com/xiaoshicae/easy-extension/wiki)
* go版本的easy-extension实现可以参考: [go-easy-extension](https://github.com/xiaoshicae/go-easy-extension)

# 代码样例
样例源码请参考: [easy-extension-sample](https://github.com/xiaoshicae/easy-extension-sample)

# License
Easy-Extension遵循Apache开源协议，具体内容请参考LICENSE文件。
