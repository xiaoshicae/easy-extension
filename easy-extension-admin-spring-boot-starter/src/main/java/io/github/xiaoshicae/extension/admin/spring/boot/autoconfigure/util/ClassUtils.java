package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import java.util.Set;
import java.util.stream.Collectors;

public class ClassUtils {

    public static Class<?> resolveClassWithAnn(Class<?> clazz, Class<? extends Annotation> annClass) {
        if (clazz == null) {
            return null;
        }
        if (clazz.isAnnotationPresent(annClass)) {
            return clazz;
        }
        return resolveClassWithAnn(clazz.getSuperclass(), annClass);
    }

    public static String classInfoToString(Class<?> clazz) {
        StringBuilder builder = new StringBuilder();

        buildAnnotation(builder, clazz.getAnnotations(), "");

        buildClassInfo(builder, clazz);

        Field[] declaredFields = clazz.getDeclaredFields();
        buildFieldInfo(builder, declaredFields);

        Method[] declaredMethods = clazz.getDeclaredMethods();
        if (declaredFields.length > 0 && declaredMethods.length > 0) {
            builder.append("\n\n");
        }
        buildMethodInfo(builder, declaredMethods);

        if (declaredFields.length > 0 || declaredMethods.length > 0) {
            builder.append("\n");
        }
        builder.append("}\n");
        return builder.toString();
    }

    private static void buildAnnotation(StringBuilder builder, Annotation[] annotations, String prefix) {
        for (Annotation ann : annotations) {
            builder.append(prefix);
            builder.append("@").append(ann.annotationType().getSimpleName());
            builder.append("\n");
        }
    }

    private static void buildClassInfo(StringBuilder builder, Class<?> clazz) {
        String classModifier = Modifier.toString(clazz.getModifiers()).trim();
        if (!classModifier.isBlank()) {
            builder.append(classModifier).append(" ");
        }

        String classType = classType(clazz);
        if (!classType.isBlank()) {
            builder.append(classType).append(" ");
        }

        String className = clazz.getSimpleName();
        builder.append(className);


        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            builder.append(" extends ").append(superclass.getSimpleName());
        }

        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length > 0) {
            builder.append(" implements ");
            for (int i = 0; i < interfaces.length; i++) {
                builder.append(interfaces[i].getSimpleName());
                if (i < interfaces.length - 1) {
                    builder.append(", ");
                }
            }
        }

        builder.append(" {\n");
    }

    private static void buildFieldInfo(StringBuilder builder, Field[] declaredFields) {
        String prefix = "    ";
        for (int i = 0; i < declaredFields.length; i++) {
            Field field = declaredFields[i];
            buildAnnotation(builder, field.getAnnotations(), prefix);

            builder.append(prefix);
            String fieldModifier = Modifier.toString(field.getModifiers()).trim();
            if (!fieldModifier.isBlank()) {
                builder.append(fieldModifier).append(" ");
            }

            String fieldType = field.getType().getSimpleName();
            builder.append(fieldType).append(" ");

            String fieldName = field.getName();
            builder.append(fieldName).append(";");

            if (i < declaredFields.length - 1) {
                builder.append("\n\n");
            }
        }
    }

    private static void buildMethodInfo(StringBuilder builder, Method[] declaredMethods) {
        String prefix = "    ";
        for (int i = 0; i < declaredMethods.length; i++) {
            Method method = declaredMethods[i];
            buildAnnotation(builder, method.getDeclaredAnnotations(), prefix);

            builder.append(prefix);
            String methodModifier = Modifier.toString(method.getModifiers()).trim();
            if (!methodModifier.isBlank()) {
                builder.append(methodModifier).append(" ");
            }

            String returnType = method.getReturnType().getSimpleName();
            builder.append(returnType).append(" ");

            String methodName = method.getName();
            builder.append(methodName).append("(");

            Parameter[] parameters = method.getParameters();
            for (int j = 0; j < parameters.length; j++) {
                Parameter p = parameters[j];
                builder.append(p.getType().getSimpleName());
                if (j < parameters.length - 1) {
                    builder.append(", ");
                }
            }

            builder.append(") {}");
            if (i < declaredMethods.length - 1) {
                builder.append("\n\n");
            }
        }
    }


    private static String classType(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return "";
        }

        if (clazz.isInterface()) {
            return "interface";
        }

        if (Modifier.isAbstract(clazz.getModifiers())) {
            return "abstract class";
        }

        return "class";
    }

    /**
     * Result holder for parsed class information.
     */
    public static class ClassInfoResult {
        private final String sourceCode;
        private final String comment;

        public ClassInfoResult(String sourceCode, String comment) {
            this.sourceCode = sourceCode;
            this.comment = comment;
        }

        public String getSourceCode() {
            return sourceCode;
        }

        public String getComment() {
            return comment;
        }
    }

    /**
     * Parse source code once and extract both class declaration and comment.
     *
     * @param sourceCode the Java source code to parse
     * @return a ClassInfoResult containing the source code and comment
     */
    public static ClassInfoResult parseClassInfo(String sourceCode) {
        ClassOrInterfaceDeclaration clazz = getClassNode(sourceCode);
        if (clazz == null) {
            return new ClassInfoResult("", "");
        }
        String comment = clazz.getComment().map(c -> c.asString()).orElse("");
        return new ClassInfoResult(clazz.toString(), comment);
    }

    /**
     * @deprecated Use {@link #parseClassInfo(String)} instead to avoid double parsing.
     */
    @Deprecated
    public static String getClass(String sourceCode) {
        return parseClassInfo(sourceCode).getSourceCode();
    }

    /**
     * @deprecated Use {@link #parseClassInfo(String)} instead to avoid double parsing.
     */
    @Deprecated
    public static String getClassComment(String sourceCode) {
        return parseClassInfo(sourceCode).getComment();
    }

    public static String transformSourceCodeWithInterface(String sourceCode, Class<?> iface) {
        Set<String> ifaceMethodSignatures = new HashSet<>();
        Method[] ifaceMethods = iface.getMethods();
        for (Method method : ifaceMethods) {
            Class<?>[] paramTypes = method.getParameterTypes();
            String signature = method.getName() + "(" + Arrays.stream(paramTypes).map(Class::getSimpleName).collect(Collectors.joining(",")) + ")";
            ifaceMethodSignatures.add(signature);
        }

        ClassOrInterfaceDeclaration clazz = getClassNode(sourceCode);
        if (clazz == null) {
            return "";
        }

        NodeList<ClassOrInterfaceType> implementedTypes = clazz.getImplementedTypes();
        implementedTypes.removeIf(e -> !e.getName().asString().equals(iface.getSimpleName()));


        List<FieldDeclaration> fields = new ArrayList<>(clazz.getFields());
        for (FieldDeclaration field : fields) {
            clazz.remove(field);
        }

        List<ClassOrInterfaceDeclaration> innerClasses = clazz.getMembers().stream()
                .filter(m -> m instanceof ClassOrInterfaceDeclaration)
                .map(m -> (ClassOrInterfaceDeclaration) m)
                .collect(Collectors.toList());
        for (ClassOrInterfaceDeclaration innerClass : innerClasses) {
            clazz.remove(innerClass);
        }

        List<MethodDeclaration> methods = new ArrayList<>(clazz.getMethods());
        for (MethodDeclaration method : methods) {
            if (!method.isPublic()) {
                clazz.remove(method);
                continue;
            }
            String paramTypesStr = method.getParameters().stream()
                    .map(p -> p.getType().asString())
                    .collect(Collectors.joining(","));
            String signature = method.getName().asString() + "(" + paramTypesStr + ")";
            if (!ifaceMethodSignatures.contains(signature)) {
                clazz.remove(method);
            }
        }

        return clazz.toString();
    }

    private static ClassOrInterfaceDeclaration getClassNode(String sourceCode) {
        // Create a new parser per invocation. Admin module has low concurrency,
        // and this avoids ThreadLocal leak in thread-pool environments.
        JavaParser parser = new JavaParser();
        ParseResult<CompilationUnit> result = parser.parse(sourceCode);
        if (!result.isSuccessful() || !result.getResult().isPresent()) {
            return null;
        }
        CompilationUnit cu = result.getResult().get();
        for (TypeDeclaration<?> type : cu.getTypes()) {
            if (type.isPublic() && type instanceof ClassOrInterfaceDeclaration clazz) {
                return clazz;
            }
        }
        return null;
    }


}
