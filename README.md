Aspectow Enterprise
===================

Aspectow is an all-in-one web application server based on Aspectran.  
Aspectow Enterprise fully supports the servlet specification and is suitable for building enterprise web applications.
JBoss's [Undertow](http://undertow.io) or Eclipse [Jetty](https://www.eclipse.org/jetty/) can be used as a web server.
[Apache Jasper](https://mvnrepository.com/artifact/org.mortbay.jasper/apache-jsp) is used to support JSP and it is the same JSP engine that Apache Tomcat uses.

## Running Aspectow

- Clone this repository

  ```sh
  $ git clone https://github.com/aspectran/aspectow-enterprise.git
  ```

- Build with Maven

  ```sh
  $ cd aspectow-enterprise
  $ mvn clean package
  ```

- Run with Aspectran Shell

  ```sh
  $ cd app/bin
  $ ./shell.sh
  ```

- Access in your browser at http://localhost:8080
