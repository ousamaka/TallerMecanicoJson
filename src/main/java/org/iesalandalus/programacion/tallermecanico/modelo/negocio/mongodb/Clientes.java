package org.iesalandalus.programacion.tallermecanico.modelo.negocio.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.Cliente;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.IClientes;

import java.util.ArrayList;
import java.util.List;

public class Clientes implements IClientes {
    private static final String COLECCION = "clientes";
    private static final String NOMBRE = "nombre";
    private static final String DNI = "dni";
    private static final String TELEFONO = "telefono";

    private MongoCollection<Document> coleccionClientes;
    private final MongoDb mongoDb;
    private static Clientes instancia;

    private Clientes() {
        mongoDb = new MongoDb();
    }

    public static Clientes getInstancia() {
        if (instancia == null) {
            instancia = new Clientes();
        }
        return instancia;
    }

    @Override
    public void comenzar() {
        mongoDb.establecerConexion();
        coleccionClientes = mongoDb.getBD().getCollection(COLECCION);
    }

    @Override
    public void terminar() {
        mongoDb.cerrarConexion();
    }

    private Cliente getCliente(Document documento) {
        if (documento == null) return null;
        return new Cliente(documento.getString(NOMBRE), documento.getString(DNI), documento.getString(TELEFONO));
    }

    private Document getDocumento(Cliente cliente) {
        if (cliente == null) return null;
        return new Document()
                .append(NOMBRE, cliente.getNombre())
                .append(DNI, cliente.getDni())
                .append(TELEFONO, cliente.getTelefono());
    }

    @Override
    public List<Cliente> get() {
        List<Cliente> clientes = new ArrayList<>();
        for (Document doc : coleccionClientes.find().sort(Sorts.ascending(DNI))) {
            clientes.add(getCliente(doc));
        }
        return clientes;
    }

    @Override
    public void insertar(Cliente cliente) throws TallerMecanicoExcepcion {
        if (cliente == null) throw new NullPointerException("No se puede insertar un cliente nulo.");
        if (buscar(cliente) != null) throw new TallerMecanicoExcepcion("El cliente ya existe.");
        coleccionClientes.insertOne(getDocumento(cliente));
    }

    @Override
    public Cliente modificar(Cliente cliente, String nombre, String telefono) throws TallerMecanicoExcepcion {
        if (cliente == null) throw new NullPointerException("No se puede modificar un cliente nulo.");
        Cliente clienteEncontrado = buscar(cliente);
        if (clienteEncontrado == null) throw new TallerMecanicoExcepcion("El cliente no existe.");

        Document filtro = new Document(DNI, cliente.getDni());
        Document actualizacion = new Document();
        if (nombre != null && !nombre.isBlank()) actualizacion.append(NOMBRE, nombre);
        if (telefono != null && !telefono.isBlank()) actualizacion.append(TELEFONO, telefono);

        if (!actualizacion.isEmpty()) {
            coleccionClientes.updateOne(filtro, new Document("$set", actualizacion));
        }
        return buscar(cliente);
    }

    @Override
    public Cliente buscar(Cliente cliente) {
        if (cliente == null) return null;
        Document doc = coleccionClientes.find(new Document(DNI, cliente.getDni())).first();
        return getCliente(doc);
    }

    @Override
    public void borrar(Cliente cliente) throws TallerMecanicoExcepcion {
        if (cliente == null) throw new NullPointerException("No se puede borrar un cliente nulo.");
        if (buscar(cliente) == null) throw new TallerMecanicoExcepcion("El cliente no existe.");

        if (!Trabajos.getInstancia().get(cliente).isEmpty()) {
            throw new TallerMecanicoExcepcion("No se puede borrar el cliente porque tiene trabajos realizados.");
        }
        coleccionClientes.deleteOne(new Document(DNI, cliente.getDni()));
    }
}