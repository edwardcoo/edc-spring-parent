package com.edc.spring.util;

import jdk.internal.org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Created by edward.coo on 2018/8/17.
 */
public class AsmUtils {
    private AsmUtils(){}
    private final static String seperator_point = ".";

    public static String[] getMethodParamNames(final Method m) {
        Class<?> clazz = m.getDeclaringClass();
        final Class<?>[] parameterTypes = m.getParameterTypes();
        if(parameterTypes==null||parameterTypes.length==0){
            return null;
        }
        final String[] paramNames = new String[parameterTypes.length];
        final Type[] types = new Type[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            types[i] = Type.getType(parameterTypes[i]);
        }
        ClassReader cr = null;
        try {
            String className = clazz.getName();
            int lastDotIndex = className.lastIndexOf(seperator_point);
            className = className.substring(lastDotIndex + 1) + ".class";
            InputStream is = clazz.getResourceAsStream(className);
            cr = new ClassReader(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cr.accept(new ClassVisitor(Opcodes.ASM4) {
            @Override
            public MethodVisitor visitMethod(final int access,
                                             final String name, final String desc,
                                             final String signature, final String[] exceptions) {
                final Type[] args = Type.getArgumentTypes(desc);
                // 方法名相同并且参数类型、个数相同
                if (!name.equals(m.getName())
                        || !Arrays.equals(args,types)) {
                    return super.visitMethod(access, name, desc, signature,
                            exceptions);
                }
//                MethodVisitor v = super.visitMethod(access, name, desc,
//                        signature, exceptions);
                return new MethodVisitor(Opcodes.ASM4) {
                    @Override
                    public void visitLocalVariable(String name, String desc,
                                                   String signature, Label start, Label end, int index) {
                        int i = index - 1;
                        // 如果是静态方法，则第一就是参数
                        // 如果不是静态方法，则第一个是"this"，然后才是方法的参数
                        if (Modifier.isStatic(m.getModifiers())) {
                            i = index;
                        }
                        if (i >= 0 && i < paramNames.length) {
                            paramNames[i] = name;
                        }
//                        super.visitLocalVariable(name, desc, signature, start,
//                                end, index);
                    }

                };
            }
        }, 0);
        return paramNames;
    }

    public static String[] getParameterNamesByAsm5(final Method method) {
        Class<?> clazz = method.getDeclaringClass();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes == null || parameterTypes.length == 0) {
            return null;
        }
        final Type[] types = new Type[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            types[i] = Type.getType(parameterTypes[i]);
        }
        final String[] parameterNames = new String[parameterTypes.length];

        String className = clazz.getName();
        int lastDotIndex = className.lastIndexOf(seperator_point);
        className = className.substring(lastDotIndex + 1) + ".class";
        InputStream is = clazz.getResourceAsStream(className);
        try {
            ClassReader classReader = new ClassReader(is);
            classReader.accept(new ClassVisitor(Opcodes.ASM5) {
                @Override
                public MethodVisitor visitMethod(int access, String name,
                                                 String desc, String signature, String[] exceptions) {
                    // 只处理指定的方法
                    Type[] argumentTypes = Type.getArgumentTypes(desc);
                    if (!method.getName().equals(name)
                            || !Arrays.equals(argumentTypes, types)) {
                        return super.visitMethod(access, name, desc, signature,
                                exceptions);
                    }
                    return new MethodVisitor(Opcodes.ASM5) {
                        @Override
                        public void visitLocalVariable(String name, String desc,
                                                       String signature, Label start,
                                                       Label end, int index) {
                            int i = index - 1;
                            // 如果是静态方法，则第一就是参数
                            // 如果不是静态方法，则第一个是"this"，然后才是方法的参数
                            if (Modifier.isStatic(method.getModifiers())) {
                                i = index;
                            }
                            if (i >= 0 && i < parameterNames.length) {
                                parameterNames[i] = name;
                            }
                        }
                    };
                }
            }, 0);
        } catch (IOException e) {
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e2) {
            }
        }
        return parameterNames;
    }

}
