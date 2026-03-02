package org.iesalandalus.programacion.tallermecanico.modelo.negocio.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.Vehiculo;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.IVehiculos;

import java.util.ArrayList;
import java.util.List;

public class Vehiculos implements IVehiculos {
    private static final String COLECCION = "vehiculos";
    private static final String MARCA = "marca";
    private static final String MODELO = "modelo";
    private static final String MATRICULA = "matricula";

    private MongoCollection<Document> coleccionVehiculos;
    private final MongoDb mongoDb;
    private static Vehiculos instancia;

    private Vehiculos() {
        mongoDb = new MongoDb();
    }

    public static Vehiculos getInstancia() {
        if (instancia == null) instancia = new Vehiculos();
        return instancia;
    }

    @Override
    public void comenzar() {
        mongoDb.establecerConexion();
        coleccionVehiculos = mongoDb.getBD().getCollection(COLECCION);
    }

    @Override
    public void terminar() {
        mongoDb.cerrarConexion();
    }

    private Vehiculo getVehiculo(Document documento) {
        if (documento == null) return null;
        return new Vehiculo(documento.getString(MARCA), documento.getString(MODELO), documento.getString(MATRICULA));
    }

    private Document getDocumento(Vehiculo vehiculo) {
        if (vehiculo == null) return null;
        return new Document()
                .append(MARCA, vehiculo.marca())
                .append(MODELO, vehiculo.modelo())
                .append(MATRICULA, vehiculo.matricula());
    }

    @Override
    public List<Vehiculo> get() {
        List<Vehiculo> vehiculos = new ArrayList<>();
        for (Document doc : coleccionVehiculos.find().sort(Sorts.ascending(MATRICULA))) {
            vehiculos.add(getVehiculo(doc));
        }
        return vehiculos;
    }

    @Override
    public void insertar(Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        if (vehiculo == null) throw new NullPointerException("No se puede insertar un vehiculo nulo.");
        if (buscar(vehiculo) != null) throw new TallerMecanicoExcepcion("El vehiculo ya existe.");
        coleccionVehiculos.insertOne(getDocumento(vehiculo));
    }

    @Override
    public Vehiculo buscar(Vehiculo vehiculo) {
        if (vehiculo == null) return null;
        Document doc = coleccionVehiculos.find(new Document(MATRICULA, vehiculo.matricula())).first();
        return getVehiculo(doc);
    }

    @Override
    public void borrar(Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        if (vehiculo == null) throw new NullPointerException("No se puede borrar un vehiculo nulo.");
        if (buscar(vehiculo) == null) throw new TallerMecanicoExcepcion("El vehiculo no existe.");

        if (!Trabajos.getInstancia().get(vehiculo).isEmpty()) {
            throw new TallerMecanicoExcepcion("No se puede borrar el vehiculo porque tiene trabajos realizados.");
        }
        coleccionVehiculos.deleteOne(new Document(MATRICULA, vehiculo.matricula()));
    }
}