Aspectow Enterprise Edition
===========================

Aspectow Enterprise Edition is an all-in-one web application server based on Aspectran,
fully supporting the servlet specification and suitable for building enterprise web applications.
It uses JBoss' [Undertow](http://undertow.io) as a servlet engine and web server, and [Apache Jasper](https://mvnrepository.com/artifact/org.mortbay.jasper/apache-jsp),
which is used in Apache Tomcat, as a JSP engine.

Running Aspectow
----------------

- Clone this repository

  ```sh
  $ git clone https://github.com/aspectran/aspectow.git
  ```

- Build with Maven

  ```sh
  $ cd aspectow
  $ mvn clean package
  ```

- Run with Aspectran Shell

  ```sh
  $ cd app/bin
  $ ./shell.sh
  ```

- Access in your browser at http://localhost:8081
