package org.iesalandalus.programacion.tallermecanico.modelo.negocio.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDb {

    private static final String SERVIDOR = "cluster0.25od2hi.mongodb.net";
    private static final int PUERTO = 27017; // Puerto por defecto de MongoDB
    private static final String BD = "tallerMecanico";
    private static final String USUARIO = "taller";
    private static final String CONTRASENA = "taller-2025";

    private MongoClient conexion;

    public MongoDb() {
    }

    public MongoDatabase getBD() {
        if (conexion == null) {
            establecerConexion();
        }
        return conexion.getDatabase(BD);
    }

    public void establecerConexion() {
        if (conexion == null) {
            String connectionString = String.format("mongodb+srv://%s:%s@%s/?retryWrites=true&w=majority",
                    USUARIO, CONTRASENA, SERVIDOR);

            ConnectionString connString = new ConnectionString(connectionString);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .build();

            conexion = MongoClients.create(settings);
        }
    }

    public void cerrarConexion() {
        if (conexion != null) {
            conexion.close();
            conexion = null;
        }
    }
}