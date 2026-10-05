export PRODUCTOS_DB_USERNAME=admin
export PRODUCTOS_DB_PASSWORD='n#4YR]KunUIfNeADU#16awZMZRfv'
export PRODUCTOS_DB_URL="jdbc:mysql://fifas-db.c0dwcxryyla0.us-east-1.rds.amazonaws.com:3306/fifas_db?sslMode=REQUIRED&serverTimezone=UTC"
export USUARIOS_DB_USERNAME=admin
export USUARIOS_DB_PASSWORD='n#4YR]KunUIfNeADU#16awZMZRfv'
export USUARIOS_DB_URL="jdbc:mysql://fifas-db.c0dwcxryyla0.us-east-1.rds.amazonaws.com:3306/fifas_db?sslMode=REQUIRED&serverTimezone=UTC"
export CARRITOS_DB_USERNAME=admin
export CARRITOS_DB_PASSWORD='n#4YR]KunUIfNeADU#16awZMZRfv'
export CARRITOS_DB_URL="jdbc:mysql://fifas-db.c0dwcxryyla0.us-east-1.rds.amazonaws.com:3306/fifas_db?sslMode=REQUIRED&serverTimezone=UTC"
nohup java -jar productoService-0.0.1-SNAPSHOT.jar --server.port=8083 > producto.log 2>&1 &
nohup java -jar usuarios-service-0.0.1-SNAPSHOT.jar --server.port=8082 > usuarios.log 2>&1 &
nohup java -jar carritoService-0.0.1-SNAPSHOT.jar --server.port=8084 > carrito.log 2>&1 &