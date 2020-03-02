package com.mvcframework.servlet.v1;

import com.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author ：yangjin.
 * @Date ：Created in 22:13 2020/3/2
 */
public class MyDispatchServlet extends HttpServlet {

    private Properties contextConfig = new Properties();
    private List<String> classNames = new ArrayList<String>();
    private Map<String, Object> ioc = new HashMap<String, Object>();
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6.调度
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail:" + Arrays.toString(e.getStackTrace()));
        }

    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //========== IOC ==============
        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2.扫描包路径，获得相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
        //3.实例化扫描到的类，放到ioc中
        doInstance();

        //========== DI ==============
        //4.完成依赖注入
        doAutowired();

        //========== MVC ==============
        //5.初始化handlerMapping
        doInitHandlerMapping();
        System.out.println("My Spring Frame init");
    }

    private void doLoadConfig(String contextConfigLocation) {
        //从类路径下读取配置文件
        InputStream is = getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replace("\\.", "/"));
        //相当于类存放包路径
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                classNames.add(scanPackage + "." + file.getName().replace(".class", ""));
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }

        try {
            for (String className : classNames) {
                Class<?> aClass = Class.forName(className);
                //类名首字母小写如果重复,在不同包里有相同类名
                //不是所有的类都需要实例化
                if (aClass.isAnnotationPresent(MyController.class)) {
                    String beanName = toLowerFirstCase(aClass.getSimpleName());
                    ioc.put(beanName, aClass.newInstance());
                } else if (aClass.isAnnotationPresent(MyServer.class)) {
                    //默认类名首字母小写
                    String beanName = toLowerFirstCase(aClass.getSimpleName());
                    //自定义beanName
                    MyServer server = aClass.getAnnotation(MyServer.class);
                    if (!"".equals(server.value())) {
                        beanName = server.value();
                    }
                    ioc.put(beanName, aClass.newInstance());
                    //将实例的类型作为beanName(有可能是接口)
                    for (Class<?> i : aClass.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The beanName is exists!!");
                        }
                        ioc.put(i.getName(), aClass.newInstance());
                    }
                } else {
                    continue;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //Declared所有的字段
            for (Field field : entry.getValue().getClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }
                MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
                String beanName = myAutowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                //暴力访问
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInitHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                //前端控制器路径处理
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
            }
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                String url = requestMapping.value();
                handlerMapping.put(url, method);
                System.out.println("Mapped:" + url + "," + method);
            }
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        uri = uri.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (this.handlerMapping.containsKey(uri)) {
            resp.getWriter().write("404 Not Found!");
            return;
        }
        Map<String, String[]> parameterMap = req.getParameterMap();
        Method method = this.handlerMapping.get(uri);
        //利用反射将方法的形参读出来
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class) {
                parameValues[i] = req;
                continue;
            } else if (parameterType == HttpServletResponse.class) {
                parameValues[i] = resp;
                continue;
            } else if (parameterType == String.class) {
                Annotation[][] pa = method.getParameterAnnotations();
                for (Annotation[] annotations : pa) {
                    for (Annotation a : pa[i]) {
                        if (a instanceof MyRequestParm) {
                            String paramName = ((MyRequestParm) a).value();
                            if (!"".equals(paramName.trim())) {
                                String value = Arrays.toString(parameterMap.get(paramName))
                                        .replaceAll("\\[|\\]", "")
                                        .replaceAll("\\s", ",");
                                parameValues[i] = value;
                            }
                        }
                    }
                }
            }
        }
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(this.ioc.get(beanName), new Object[]{
                req, resp, parameValues
        });
    }
}
