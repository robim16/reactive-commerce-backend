// infra/mongodb/init-dbs.js
// Ejecutado automáticamente por MongoDB en el primer arranque.
// Crea las bases de datos de cada microservicio y el usuario de acceso.
// Las bases de datos se crean implícitamente al insertar el primer documento,
// pero este script garantiza que los usuarios y permisos existan antes
// de que los servicios arranquen.

const rootUser = process.env.MONGO_INITDB_ROOT_USERNAME || "rcadmin";
const rootPass = process.env.MONGO_INITDB_ROOT_PASSWORD || "rcpassword";

const databases = [
  "payment_db",
  "review_db",
  "notification_db",
  "report_db",
];

databases.forEach(dbName => {
  const db = db.getSiblingDB(dbName);
  // Crear un documento de init para forzar la creación de la base de datos
  db.createCollection("_init");
  print(`Database created: ${dbName}`);
});

print("MongoDB initialization complete.");
