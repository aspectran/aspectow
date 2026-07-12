CREATE SCHEMA IF NOT EXISTS console;
RUNSCRIPT FROM 'classpath:com/aspectran/aspectow/console/config/db/aspectow-console-schema-h2.sql';
RUNSCRIPT FROM 'classpath:com/aspectran/aspectow/appmon/config/db/appmon-schema-h2.sql';
