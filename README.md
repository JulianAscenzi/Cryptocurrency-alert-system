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

### API REST básica

El prototipo expone una API REST para crear, consultar y cancelar alertas.

Endpoints principales:

- `POST /alerts`
  - Crea una alerta nueva.
  - Body JSON: `{ "symbol": "BTC", "targetPrice": 50000, "condition": "ABOVE" }`
- `GET /alerts`
  - Devuelve la lista de alertas activas.
- `GET /alerts/{id}`
  - Devuelve una alerta por su id.
- `DELETE /alerts/{id}`
  - Cancela una alerta activa.

Si necesitas añadir nuevas extensiones a un proyecto existente, usa:

```bash
mvn quarkus:add-extension -Dextensions="quarkus-resteasy-reactive,quarkus-resteasy-reactive-jackson,quarkus-websockets-next"
```
