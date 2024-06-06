# Easy-Extension
Easy-Extension是解决复杂系统扩展性问题的框架，适用于多接入方，且有定制化扩展诉求的场景。例如电商交易，履约等中台系统。

# 框架解决的业务场景
![](/doc/target.png)

# 框架使用Demo
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

# 文档
完整文档请参考: [wiki](https://github.com/xiaoshicae/easy-extension/wiki)

# 代码样例
样例源码请参考: [easy-extension-sample](https://github.com/xiaoshicae/easy-extension-sample)

# License
Easy-Extension遵循Apache开源协议，具体内容请参考LICENSE文件。
