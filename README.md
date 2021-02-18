# TongWeb CGI Servlet Implement

## 背景
TongWeb6.0中的CGI Servlet只实现了get 和 post方法。 如果需要put、delete或者options等其他方法。会被TongWeb拦截掉。

因此，为了能够友好的支持其他http请求方法。重新实现了一个http servlet。

## 开发环境

JDK 1.8

## 使用

1. 编译之后，将honray-cgi放在TongWeb的WEB-INF的classes目录下
2. 修改web.xml文件，servlet 配置。参考配置如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="3.0" xmlns="http://java.sun.com/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
                                http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd">
    <servlet>
        <servlet-name>cgi</servlet-name>
        <servlet-class>com.honray.tongweb.servlet.HonrayCGIServlet</servlet-class>
        <init-param>
            <param-name>debug</param-name>
            <param-value>1</param-value>
        </init-param>
        <init-param>
            <param-name>cgiPathPrefix</param-name>
            <param-value>public</param-value>
        </init-param>
        <init-param>
            <param-name>executable</param-name>
            <param-value>php-cgi</param-value>
        </init-param>
        <init-param>
            <param-name>passShellEnvironment</param-name>
            <param-value>true</param-value>
        </init-param>
        <load-on-startup>5</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>cgi</servlet-name>
        <url-pattern>/cgi/</url-pattern>
    </servlet-mapping>
</web-app>
```


