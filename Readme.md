POST WITH-NULL-LIBRARY-EVENT-ID
---------------------
curl -i \
-d '{"id":null,"book":{"id":456,"name":"Kafka Using Spring Boot","author":"Dazo"}}' \
-H "Content-Type: application/json" \
-X POST http://localhost:8080/v1/libraryevent

PUT WITH ID - 1
--------------
curl -i \
-d '{"id":1,"book":{"id":456,"name":"Kafka Using Spring Boot 2.X","author":"Dazo"}}' \
-H "Content-Type: application/json" \
-X PUT http://localhost:8080/v1/libraryevent

curl -i \
-d '{"id":2,"book":{"id":456,"name":"Kafka Using Spring Boot 2.X","author":"Dazo"}}' \
-H "Content-Type: application/json" \
-X PUT http://localhost:8080/v1/libraryevent



PUT WITH ID
---------------------
curl -i \
-d '{"id":123,"book":{"id":456,"name":"Kafka Using Spring Boot","author":"Dazo"}}' \
-H "Content-Type: application/json" \
-X PUT http://localhost:8080/v1/libraryevent

curl -i \
-d '{"id":999,"book":{"id":456,"name":"Kafka Using Spring Boot","author":"Dazo"}}' \
-H "Content-Type: application/json" \
-X PUT http://localhost:8080/v1/libraryevent

PUT WITHOUT ID
---------------------
curl -i \
-d '{"id":null,"book":{"id":456,"name":"Kafka Using Spring Boot","author":"Dazo"}}' \
-H "Content-Type: application/json" \
-X PUT http://localhost:8080/v1/libraryevent