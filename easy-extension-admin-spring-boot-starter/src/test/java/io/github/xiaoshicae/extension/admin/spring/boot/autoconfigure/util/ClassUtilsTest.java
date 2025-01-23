package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util;

import io.github.xiaoshicae.extension.core.annotation.Ability;
import io.github.xiaoshicae.extension.core.annotation.Business;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.common.Identifier;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClassUtilsTest {
    @Test
    public void testClassInfoToString() throws Exception {
        String classInfo = ClassUtils.classInfoToString(TypeInfoTestClass.class);
        String expectedClassInfo = """
                @ExtensionPoint
                class TypeInfoTestClass extends SClass implements Identifier {
                    private String a;
                                
                    private Integer b;
                                
                    private Double c;
                                
                    private int d;
                                
                    private List e;
                                
                    private TypeInfoClassPropertyTest propertyTest;
                                
                    @Value
                    DynamicNode dn;
                                
                    protected Object clone() {}
                                
                    public String code() {}
                                
                    @Value
                    @Validated
                    String X() {}
                                
                    public String XXX() {}
                                
                    private String XX() {}
                                
                    protected String XXXX() {}
                }
                """;
        assertTrue(classInfo.contains("@ExtensionPoint"));
        assertTrue(classInfo.contains("protected String XXXX() {}"));
    }


    @Test
    public void testResolveClassWithAnn() throws Exception {
        Class<?> clazz = ClassUtils.resolveClassWithAnn(TypeInfoTestClass.class, Ability.class);
        assertNull(clazz);

        clazz = ClassUtils.resolveClassWithAnn(MyBusiness.class, Business.class);
        assertEquals(clazz, MyBusiness.class);

        clazz = ClassUtils.resolveClassWithAnn(MyBusiness1.class, Business.class);
        assertEquals(clazz, MyBusiness.class);

        IBusiness<Object> b = new MyBusiness1();
        clazz = ClassUtils.resolveClassWithAnn(b.getClass(), Business.class);
        assertEquals(clazz, MyBusiness.class);
    }

    @Test
    public void testResolveClassWithAnnotation() throws Exception {
        String[] prefixes = new String[]{"/**", "*/", "*",  "//"};
        String sourceCode = """
                /**
                 * 扩展点，必须标注
                 * @ExtensionPoint
                 */
                """;
        String[] items  = sourceCode.split("\n");
        StringBuilder builder = new StringBuilder();

        for (String item : items) {
            item = item.strip();
            for (String p : prefixes) {
                if (item.startsWith(p)) {
                    String newItem = item.substring(p.length());
                    if (!newItem.isEmpty()) {
                        builder.append(newItem.strip());
                        builder.append("\n");
                    }
                    break;
                }
            }
        }

        System.out.println(builder.toString());
    }

    @Test
    public void testX() throws Exception {
        String s = ClassUtils.transformSourceCodeWithInterface(fileContent, Extension1.class);
//        System.out.println(s);

        s = ClassUtils.getClassComment(fileContent);
        System.out.println(s);
    }

    private String fileContent = """
            package io.github.xiaoshicae.extension.sample.complex.extpoint;
                        
            import io.github.xiaoshicae.extension.core.annotation.ExtensionPointDefaultImplementation;
                        
                        
            /**
             * 扩展点的默认实现(必须实现所有的扩展点)，必须标注@ExtensionPointDefaultImplementation
             */
            @ExtensionPointDefaultImplementation
            public class ExtDefaultImpl implements Extension1,  Extension2, Extension3 {
                private String s;
                private Integer i;
                
                /**
                 *
                 * hhhh
                 *
                 */
                @Override
                public String ext1DoSomeThing(String p, Integer p1, Integer p2) {
                    class InnerMethodClass {}
                    
                    if (p=="") {
                        return "hhhh";
                    }
                    return "default ext1DoSomeThing " + p;
                }
                      
                // 哈哈哈        
                @Override
                public String ext2DoSomeThing() {
                    return "default ext2DoSomeThing";
                }
                        
                @Override
                public void ext3DoSomeThing(Boolean b) {
                    return 0;
                }
                
                class InnerClass {}
                
                private String xxx() {}
            }
                        
            class OuterClass {
                        
            }            
            """;
}

class SClass {

}

@ExtensionPoint
class TypeInfoTestClass extends SClass implements Identifier {
    private String a;
    private Integer b;
    private Double c;
    private int d;
    private List<?> e;
    private TypeInfoClassPropertyTest propertyTest;

    @Value("123")
    DynamicNode dn;

    public TypeInfoTestClass() {
    }

    @Value("123")
    @Validated
    String X() {
        return "123";
    }

    private String XX() {
        return "123";
    }

    protected String XXXX() {
        return "123";
    }

    public String XXX() {
        return "123";
    }


    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String code() {
        return "";
    }
}


@Ability(code = "")
@Business(code = "")
class TypeInfoClassPropertyTest {
    private String aa;
    private Integer bb;
    private Double cc;
    private int dd;

    List<TypeInfoClassPropertyTest> propertyTests;
}

@Business(code = "code")
class MyBusiness {
}

class MyBusiness1 extends MyBusiness implements IBusiness<Object> {
    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of();
    }

    @Override
    public Integer priority() {
        return 0;
    }

    @Override
    public List<Class<?>> implementExtensionPoints() {
        return List.of();
    }

    @Override
    public String code() {
        return "";
    }

    @Override
    public Boolean match(Object param) {
        return null;
    }
}


/**
 * 扩展点，必须标注@ExtensionPoint
 */
@ExtensionPoint
 interface Extension1 {
    String ext1DoSomeThing(String p, Integer p1, Integer p2);
}
