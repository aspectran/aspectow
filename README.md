Aspectow Enterprise Edition
===========================

Aspectow Enterprise Edition is an all-in-one web application server based on Aspectran,
fully supports servlet specifications, and is suitable for building enterprise web applications.  
JBoss' [Undertow](http://undertow.io) web server and [Apache Jasper](https://mvnrepository.com/artifact/org.mortbay.jasper/apache-jsp), 
the JSP engine used by Apache Tomcat, are built in.

## Running Aspectow

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
