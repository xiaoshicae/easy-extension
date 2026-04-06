package io.github.xiaoshicae.extension.intellij.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.ClassInheritorsSearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * PSI 搜索工具类，封装与 Easy-Extension 框架相关的代码搜索方法
 */
public final class PsiSearchUtil {

    private PsiSearchUtil() {
    }

    /**
     * 查找项目中所有标注了 @ExtensionPoint 的接口
     */
    public static Collection<PsiClass> findAllExtensionPoints(Project project) {
        return findClassesWithAnnotation(project, EasyExtensionAnnotations.EXTENSION_POINT);
    }

    /**
     * 查找项目中所有标注了 @Ability 的类
     */
    public static Collection<PsiClass> findAllAbilities(Project project) {
        return findClassesWithAnnotation(project, EasyExtensionAnnotations.ABILITY);
    }

    /**
     * 查找项目中所有标注了 @Business 的类
     */
    public static Collection<PsiClass> findAllBusinesses(Project project) {
        return findClassesWithAnnotation(project, EasyExtensionAnnotations.BUSINESS);
    }

    /**
     * 查找项目中所有标注了 @ExtensionPointDefaultImplementation 的类
     */
    public static Collection<PsiClass> findAllDefaultImpls(Project project) {
        return findClassesWithAnnotation(project, EasyExtensionAnnotations.DEFAULT_IMPLEMENTATION);
    }

    /**
     * 查找某个扩展点接口的所有实现类（包含 @Ability、@Business、@ExtensionPointDefaultImplementation）
     */
    public static List<PsiClass> findExtensionPointImplementations(PsiClass extensionPointClass) {
        Project project = extensionPointClass.getProject();
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        Collection<PsiClass> allInheritors = ClassInheritorsSearch.search(extensionPointClass, scope, true).findAll();

        List<PsiClass> result = new ArrayList<>();
        for (PsiClass inheritor : allInheritors) {
            if (hasAnnotation(inheritor, EasyExtensionAnnotations.ABILITY)
                    || hasAnnotation(inheritor, EasyExtensionAnnotations.BUSINESS)
                    || hasAnnotation(inheritor, EasyExtensionAnnotations.DEFAULT_IMPLEMENTATION)) {
                result.add(inheritor);
            }
        }

        // 按匹配优先级排序：Business > Ability > DefaultImpl
        result.sort((a, b) -> getAnnotationOrder(a) - getAnnotationOrder(b));
        return result;
    }

    /**
     * 获取注解类型的排序权重，值越小优先级越高
     */
    private static int getAnnotationOrder(PsiClass psiClass) {
        if (hasAnnotation(psiClass, EasyExtensionAnnotations.BUSINESS)) {
            return 0;
        }
        if (hasAnnotation(psiClass, EasyExtensionAnnotations.ABILITY)) {
            return 1;
        }
        if (hasAnnotation(psiClass, EasyExtensionAnnotations.DEFAULT_IMPLEMENTATION)) {
            return 2;
        }
        return 3;
    }

    /**
     * 获取某个类实现的所有 @ExtensionPoint 接口（迭代方式，防止栈溢出）
     */
    public static List<PsiClass> findImplementedExtensionPoints(PsiClass psiClass) {
        List<PsiClass> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        PsiClass current = psiClass;
        while (current != null && !"java.lang.Object".equals(current.getQualifiedName())) {
            String fqn = current.getQualifiedName();
            if (fqn != null && !visited.add(fqn)) {
                break;
            }
            for (PsiClass iface : current.getInterfaces()) {
                if (hasAnnotation(iface, EasyExtensionAnnotations.EXTENSION_POINT)) {
                    result.add(iface);
                }
            }
            current = current.getSuperClass();
        }
        return result;
    }

    /**
     * 从 @ExtensionInject 字段的类型中解析出扩展点接口
     * 支持直接类型和 List<T> 类型
     */
    public static PsiClass resolveExtensionPointFromField(PsiField field) {
        PsiType type = field.getType();
        PsiClass resolved = resolveExtensionPointClass(type);
        if (resolved != null && hasAnnotation(resolved, EasyExtensionAnnotations.EXTENSION_POINT)) {
            return resolved;
        }
        return null;
    }

    /**
     * 从注解属性中读取字符串值（支持字符串字面量和常量引用）
     */
    public static String getAnnotationStringValue(PsiAnnotation annotation, String attributeName) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
        if (value == null) {
            return null;
        }
        // 字符串字面量
        if (value instanceof PsiLiteralExpression literal && literal.getValue() instanceof String s) {
            return s;
        }
        // 常量引用
        if (value instanceof PsiReferenceExpression ref) {
            PsiElement resolved = ref.resolve();
            if (resolved instanceof PsiField field) {
                Object constVal = field.computeConstantValue();
                if (constVal instanceof String s) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * 从 @Business 注解的 abilities 属性中解析出能力 code 列表（使用 PSI API 解析）
     */
    public static List<String> parseAbilityCodes(PsiAnnotation businessAnnotation) {
        PsiAnnotationMemberValue abilitiesValue = businessAnnotation.findAttributeValue("abilities");
        if (abilitiesValue == null) {
            return Collections.emptyList();
        }

        List<PsiAnnotationMemberValue> elements = new ArrayList<>();
        if (abilitiesValue instanceof PsiArrayInitializerMemberValue arrayValue) {
            elements.addAll(Arrays.asList(arrayValue.getInitializers()));
        } else {
            elements.add(abilitiesValue);
        }

        List<String> codes = new ArrayList<>();
        for (PsiAnnotationMemberValue elem : elements) {
            String raw = resolveStringValue(elem);
            if (raw != null && !raw.isEmpty()) {
                // 去除优先级部分 "code::priority" → "code"
                int sep = raw.indexOf("::");
                codes.add(sep > 0 ? raw.substring(0, sep) : raw);
            }
        }
        return codes;
    }

    /**
     * 从 @Business 注解的 abilities 属性中解析出能力 code 和优先级（格式："code::priority"）
     */
    public static List<String> parseAbilityRawValues(PsiAnnotation businessAnnotation) {
        PsiAnnotationMemberValue abilitiesValue = businessAnnotation.findAttributeValue("abilities");
        if (abilitiesValue == null) {
            return Collections.emptyList();
        }

        List<PsiAnnotationMemberValue> elements = new ArrayList<>();
        if (abilitiesValue instanceof PsiArrayInitializerMemberValue arrayValue) {
            elements.addAll(Arrays.asList(arrayValue.getInitializers()));
        } else {
            elements.add(abilitiesValue);
        }

        List<String> rawValues = new ArrayList<>();
        for (PsiAnnotationMemberValue elem : elements) {
            String raw = resolveStringValue(elem);
            if (raw != null && !raw.isEmpty()) {
                rawValues.add(raw);
            }
        }
        return rawValues;
    }

    /**
     * 根据能力 code 查找对应的 @Ability 类
     */
    public static PsiClass findAbilityByCode(Project project, String abilityCode) {
        Collection<PsiClass> abilities = findAllAbilities(project);
        for (PsiClass ability : abilities) {
            PsiAnnotation ann = ability.getAnnotation(EasyExtensionAnnotations.ABILITY);
            if (ann != null) {
                String code = getAnnotationStringValue(ann, "code");
                if (abilityCode.equals(code)) {
                    return ability;
                }
            }
        }
        return null;
    }

    /**
     * 为扩展点生成实现摘要文本（用于 tooltip 显示）
     */
    public static String buildImplementationSummary(PsiClass extensionPointClass) {
        List<PsiClass> impls = findExtensionPointImplementations(extensionPointClass);
        if (impls.isEmpty()) {
            return "No implementations";
        }

        List<String> abilities = new ArrayList<>();
        List<String> businesses = new ArrayList<>();
        String defaultImpl = null;

        for (PsiClass impl : impls) {
            String name = Objects.requireNonNullElse(impl.getName(), "?");
            if (hasAnnotation(impl, EasyExtensionAnnotations.DEFAULT_IMPLEMENTATION)) {
                defaultImpl = name;
            } else if (hasAnnotation(impl, EasyExtensionAnnotations.ABILITY)) {
                PsiAnnotation ann = impl.getAnnotation(EasyExtensionAnnotations.ABILITY);
                String code = ann != null ? getAnnotationStringValue(ann, "code") : null;
                abilities.add(code != null ? name + "(" + code + ")" : name);
            } else if (hasAnnotation(impl, EasyExtensionAnnotations.BUSINESS)) {
                PsiAnnotation ann = impl.getAnnotation(EasyExtensionAnnotations.BUSINESS);
                String code = ann != null ? getAnnotationStringValue(ann, "code") : null;
                businesses.add(code != null ? name + "(" + code + ")" : name);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        if (!businesses.isEmpty()) {
            sb.append("<b>Business:</b> ").append(String.join(", ", businesses)).append("<br/>");
        }
        if (!abilities.isEmpty()) {
            sb.append("<b>Ability:</b> ").append(String.join(", ", abilities)).append("<br/>");
        }
        if (defaultImpl != null) {
            sb.append("<b>Default:</b> ").append(defaultImpl);
        }
        sb.append("</html>");
        return sb.toString();
    }

    /**
     * 检查 PsiClass 是否有指定注解
     */
    public static boolean hasAnnotation(PsiClass psiClass, String annotationFqn) {
        return psiClass.getAnnotation(annotationFqn) != null;
    }

    /**
     * 查找所有标注了指定注解的类
     * 注意：注解类在 allScope 中查找（含依赖库），被标注的类在 projectScope 中查找（仅项目源码）
     */
    private static Collection<PsiClass> findClassesWithAnnotation(Project project, String annotationFqn) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        PsiClass annotationClass = JavaPsiFacade.getInstance(project)
                .findClass(annotationFqn, GlobalSearchScope.allScope(project));
        if (annotationClass == null) {
            return Collections.emptyList();
        }
        return AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope).findAll();
    }

    private static PsiClass resolveExtensionPointClass(PsiType type) {
        if (type instanceof PsiClassType classType) {
            PsiClass resolved = classType.resolve();
            if (resolved == null) {
                return null;
            }
            // 处理 List<ExtPoint> 类型
            String qName = resolved.getQualifiedName();
            if ("java.util.List".equals(qName) || "java.util.Collection".equals(qName)) {
                PsiType[] typeParams = classType.getParameters();
                if (typeParams.length == 1 && typeParams[0] instanceof PsiClassType innerType) {
                    return innerType.resolve();
                }
                return null;
            }
            return resolved;
        }
        return null;
    }

    /**
     * 解析 PsiAnnotationMemberValue 为字符串值（支持字面量和常量引用）
     */
    private static String resolveStringValue(PsiAnnotationMemberValue value) {
        if (value instanceof PsiLiteralExpression literal && literal.getValue() instanceof String s) {
            return s;
        }
        if (value instanceof PsiReferenceExpression ref) {
            PsiElement resolved = ref.resolve();
            if (resolved instanceof PsiField field) {
                Object constVal = field.computeConstantValue();
                if (constVal instanceof String s) {
                    return s;
                }
            }
        }
        return null;
    }
}
