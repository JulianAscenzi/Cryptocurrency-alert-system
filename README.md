# Cryptocurrency-alert-system

Set up an alert when there is a change in a cryptocurrency

## Maven

Este repositorio ya es un proyecto Maven de Quarkus, por lo que no es necesario usar `mvn io.quarkus:quarkus-maven-plugin:create` aquí.

Para ejecutar en modo desarrollo:

```bash
mvn quarkus:dev
```

Para compilar y empaquetar:

```bash
mvn clean package
```

Si necesitas añadir nuevas extensiones a un proyecto existente, usa:

```bash
mvn quarkus:add-extension -Dextensions="quarkus-resteasy-reactive,quarkus-resteasy-reactive-jackson,quarkus-websockets-next"
```
