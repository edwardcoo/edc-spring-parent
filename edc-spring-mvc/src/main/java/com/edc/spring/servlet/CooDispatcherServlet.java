package com.edc.spring.servlet;

import com.edc.spring.HandlerMethod;
import com.edc.spring.annotation.*;
import com.edc.spring.util.AsmUtils;
import com.edc.spring.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by edward.coo on 2018/8/9.
 */
public class CooDispatcherServlet extends HttpServlet {

    private final static String seperator = "/";
    private final static String seperator_regex = "/+";
    private final static String seperator_point = ".";
    private List<String> classNames = new ArrayList<String>();//存放class类
    private Map<String, Object> beansMap = new ConcurrentHashMap<String, Object>();//ioc容器，存放bean instance
    private Map<String, HandlerMethod> handlerMapping = new ConcurrentHashMap<String, HandlerMethod>();//handlerMapping，存放url-method关系

    public CooDispatcherServlet() {
        System.out.println("开始启动了...");
    }

    @Override
    public void init() throws ServletException {
        //1.扫描class文件
        doScanPackage("com.edc");
        //2.实例化bean
        doInstance();
        //3.handlerMapping
        doHandlerMapping();
        //4.注入
        try {
            doIoc();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void doIoc() throws IllegalAccessException {
        if (beansMap.isEmpty()) {
            System.out.println("没有需要Ioc的实例...");
            return;
        }
        for (Map.Entry<String, Object> entry : beansMap.entrySet()) {
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();
            if (clazz.isAnnotationPresent(CooController.class) || clazz.isAnnotationPresent(CooService.class) || clazz.isAnnotationPresent(CooComponent.class)) {
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field field : declaredFields) {
                    Object bean = null;
                    if (field.isAnnotationPresent(CooResource.class)) {
                        CooResource annotation = field.getAnnotation(CooResource.class);
                        String name = annotation.name();//testService
                        if (StringUtils.isNotEmpty(name)) {
                            //根据名称
                            bean = getBean(name);
                        } else {
                            //根据类型
                            bean = getBean(field.getType());
                        }
                    } else if (field.isAnnotationPresent(CooAutowired.class)) {
                        //根据类型
                        bean = getBean(field.getType());
                    }
                    if (bean != null) {
                        field.setAccessible(true);
                        field.set(instance, bean);
                    }
                }
            }
        }
    }

    public Object getBean(String name) {
        Object bean = beansMap.get(name);
        if (bean == null) {
            throw new RuntimeException("没有找到bean[" + name + "]");//NoSuchBeanDefinitionException
        }
        return bean;
    }

    public <T> T getBean(String name, Class<T> requiredType) {
        Object bean = beansMap.get(name);
        if (requiredType != null && !requiredType.isAssignableFrom(bean.getClass())) {
            throw new RuntimeException("找到bean与需求bean类型不一致");//BeanNotOfRequiredTypeException
        }
        return (T) bean;
    }

    public <T> T getBean(Class<T> requiredType) {
        String[] beanNames = getBeanNamesForType(requiredType);
        if (beanNames.length == 1) {
            return getBean(beanNames[0], requiredType);
        } else if (beanNames.length > 1) {
            throw new RuntimeException("想要一个却找到多个bean," + Arrays.toString(beanNames));//NoUniqueBeanDefinitionException
        } else {
            throw new RuntimeException("没有找到bean" + requiredType.getName());//NoSuchBeanDefinitionException
        }
    }

    private String[] getBeanNamesForType(Class<?> type) {
        List<String> matches = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : beansMap.entrySet()) {
            String name = entry.getKey();
            Object beanInstance = entry.getValue();
            if (type.isAssignableFrom(beanInstance.getClass())) {
                matches.add(name);
            }
        }
        return StringUtils.toStringArray(matches);
    }

    private void doHandlerMapping() {
        if (beansMap.isEmpty()) {
            System.out.println("没有需要mapping的实例...");
            return;
        }
        for (Map.Entry<String, Object> entry : beansMap.entrySet()) {
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();
            if (clazz.isAnnotationPresent(CooController.class)) {
                //类基础url
                List<String> baseUrlList = new ArrayList<String>();
                if(clazz.isAnnotationPresent(CooRequestMapping.class)){
                    CooRequestMapping requestMappingType = clazz.getAnnotation(CooRequestMapping.class);
                    baseUrlList = handlerUrls(requestMappingType.value());
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (!method.isAnnotationPresent(CooRequestMapping.class)) {
                        continue;
                    }
                    CooRequestMapping requestMappingMethod = method.getAnnotation(CooRequestMapping.class);
                    List<String> methodUrlList = handlerUrls(requestMappingMethod.value());
                    for (String methodUrl : methodUrlList) {
                        String methodKey = StringUtils.isEmpty(methodUrl) ? seperator : seperator + methodUrl + seperator;// /test/method/.../
                        if (baseUrlList.isEmpty()) {
                            putMapping(methodKey, method, instance);
                            continue;
                        }
                        for (String baseUrl : baseUrlList) {
                            String fullKey = StringUtils.isEmpty(baseUrl) ? methodKey : seperator + baseUrl + methodKey;
                            putMapping(fullKey, method, instance);
                        }
                    }
                }

            }
        }
    }

    private void putMapping(String key, Method method, Object instance) {
        if (handlerMapping.containsKey(key)) {
            throw new RuntimeException("repeat url mapping with [" + key + "]");
        }
        HandlerMethod handlerMethod = new HandlerMethod(method,instance);
        handlerMapping.put(key, handlerMethod);
    }

    private List<String> handlerUrls(String[] urls) {
        List<String> urlList = new ArrayList<String>();
        if (urls == null) {
            return urlList;
        }
        for (String url : urls) {
            if (url == null) {
                continue;
            }
            url = url.replaceAll(seperator_regex,seperator);
            if (url.startsWith(seperator)) {
                url = url.substring(1);
            }
            if (url.endsWith(seperator)) {
                url = url.substring(0, url.length() - 1);
            }
            urlList.add(url);
        }
        return urlList;
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            System.out.println("没有需要实例化的类...");
            return;
        }
        for (String className : classNames) {
            try {
                String name = className.substring(0, className.length() - 6);
                Class<?> clazz = Class.forName(name);
                String key = StringUtils.toLowerCaseFirst(clazz.getSimpleName());//TestServiceImpl==>testServiceImpl
                Object instance = null;
                if (clazz.isAnnotationPresent(CooController.class)) {
                    CooController annotation = clazz.getAnnotation(CooController.class);
                    if (StringUtils.isNotEmpty(annotation.value())) {
                        key = annotation.value();
                    }
                    instance = clazz.newInstance();
                } else if (clazz.isAnnotationPresent(CooService.class)) {
                    CooService annotation = clazz.getAnnotation(CooService.class);
                    if (StringUtils.isNotEmpty(annotation.value())) {
                        key = annotation.value();
                    }
                    instance = clazz.newInstance();
                } else if (clazz.isAnnotationPresent(CooComponent.class)) {
                    CooComponent annotation = clazz.getAnnotation(CooComponent.class);
                    if (StringUtils.isNotEmpty(annotation.value())) {
                        key = annotation.value();
                    }
                    instance = clazz.newInstance();
                } else if (clazz.isAnnotationPresent(CooRepository.class)) {
                    CooRepository annotation = clazz.getAnnotation(CooRepository.class);
                    if (StringUtils.isNotEmpty(annotation.value())) {
                        key = annotation.value();
                    }
                    instance = clazz.newInstance();
                }
                if (StringUtils.isNotEmpty(key) && instance != null) {
                    beansMap.put(key, instance);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }


    private void doScanPackage(String basePackage) {//com.edc==>/com/edc
        String scanPath = seperator + basePackage.replaceAll("\\.", seperator);
        URL url = this.getClass().getClassLoader().getResource(scanPath);
        String basePath = url.getFile();
        File basePathFile = new File(basePath);
        String[] list = basePathFile.list();
        for (String fileName : list) {
            File file = new File(basePath + fileName);
            if (file.isDirectory()) {
                doScanPackage(basePackage + seperator_point + fileName);
            } else if (fileName.endsWith(".class")) {
                classNames.add(basePackage + seperator_point + fileName);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("收到请求:--> " + req.getRequestURL() + "?" + req.getQueryString());
        //处理请求
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String urlPath = uri.replace(contextPath, "").replaceAll(seperator_regex,seperator);
        if (!urlPath.startsWith(seperator)) {
            urlPath = seperator + urlPath;
        }
        if (!urlPath.endsWith(seperator)) {
            urlPath = urlPath + seperator;
        }
        HandlerMethod handlerMethod = handlerMapping.get(urlPath);
        if(handlerMethod == null){
            throw new RuntimeException("not found handler for url["+urlPath+"]");
        }
        Method method = handlerMethod.getMethod();
        Object instance = handlerMethod.getBean();
        Object[] args = handleMethodParams(req, resp, method);
        try {
            Object result = method.invoke(instance, args);
            handleResponseBody(req, resp, method, result);
            System.out.println("处理完毕-->" + result);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void handleResponseBody(HttpServletRequest request, HttpServletResponse response, Method method, Object result) {
        if (method.isAnnotationPresent(CooResponseBody.class)) {
            try {
                response.setHeader("Content-type","text/html;charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(result.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Object[] handleMethodParams(HttpServletRequest req, HttpServletResponse resp, Method method) {
        //拿到当前执行方法有哪些参数
        Class<?>[] parameterClazzs = method.getParameterTypes();
        if (parameterClazzs == null || parameterClazzs.length == 0) {
            return new Object[]{};
        }
        //根据参数的个数，创建一个参数的数组，将方法里的所有参数赋值到数组里面
        Object[] args = new Object[parameterClazzs.length];
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        String[] paramNames = AsmUtils.getMethodParamNames(method);
        int args_i = 0;
        for (Class<?> parameterClazz : parameterClazzs) {
            if (ServletRequest.class.isAssignableFrom(parameterClazz)) {
                args[args_i++] = req;
                continue;
            }
            if (ServletResponse.class.isAssignableFrom(parameterClazz)) {
                args[args_i++] = resp;
                continue;
            }
            String name = paramNames[args_i];
            boolean required = false;
            Annotation[] annotations = parameterAnnotations[args_i];
            for (Annotation annotation : annotations) {
                if (CooRequestParam.class.isAssignableFrom(annotation.annotationType())) {
                    CooRequestParam cooRequestParam = (CooRequestParam) annotation;
                    if (StringUtils.isNotEmpty(cooRequestParam.value())) {
                        name = cooRequestParam.value();
                    }
                    required = cooRequestParam.required();
                }
            }
            Object value = getParamValue(parameterClazz, req, name);
            if (required && value == null) {
                throw new RuntimeException("the paramter [" + name + "] must not be null");
            }
            args[args_i++] = value;
        }
        return args;
    }

    private Object getParamValue(Class<?> parameterClazz, HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        if (Integer.class.isAssignableFrom(parameterClazz)) {
            return Integer.valueOf(value);
        } else if (Long.class.isAssignableFrom(parameterClazz)) {
            return Long.valueOf(value);
        } else if (Double.class.isAssignableFrom(parameterClazz)) {
            return Double.valueOf(value);
        } else if (Float.class.isAssignableFrom(parameterClazz)) {
            return Float.valueOf(value);
        } else if (BigDecimal.class.isAssignableFrom(parameterClazz)) {
            return new BigDecimal(value);
        }
        return value;
    }
    public static void main(String[] args) {
        System.out.println(111);
    }

}
