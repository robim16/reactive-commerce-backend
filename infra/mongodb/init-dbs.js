// infra/mongodb/init-dbs.js
// Ejecutado automáticamente por MongoDB en el primer arranque.
// Coloca este ARCHIVO (no carpeta) en infra/mongodb/init-dbs.js

const rootUser = process.env.MONGO_INITDB_ROOT_USERNAME || "rcadmin";
const rootPass = process.env.MONGO_INITDB_ROOT_PASSWORD || "rcpassword";

const databases = [
  "payment_db",
  "review_db",
  "notification_db",
  "report_db",
];

databases.forEach(function(dbName) {
  const targetDb = db.getSiblingDB(dbName);
  targetDb.createCollection("_init");
  targetDb["_init"].insertOne({
    initialized: true,
    createdAt: new Date()
  });
  print("Database initialized: " + dbName);
});

print("MongoDB initialization complete.");
