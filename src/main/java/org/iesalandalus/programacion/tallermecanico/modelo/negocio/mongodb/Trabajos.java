package org.iesalandalus.programacion.tallermecanico.modelo.negocio.mongodb;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.*;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.ITrabajos;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Trabajos implements ITrabajos {
    private static final String COLECCION = "trabajos";
    private static final String FECHA_INICIO = "fechaInicio";
    private static final String FECHA_FIN = "fechaFin";
    private static final String TIPO = "tipo";
    private static final String HORAS = "horas";
    private static final String PRECIO_MATERIAL = "precioMaterial";
    private static final String DNI_CLIENTE = "dniCliente";
    private static final String MATRICULA_VEHICULO = "matriculaVehiculo";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private MongoCollection<Document> coleccionTrabajos;
    private final MongoDb mongoDb;
    private static Trabajos instancia;

    private Trabajos() {
        mongoDb = new MongoDb();
    }

    public static Trabajos getInstancia() {
        if (instancia == null) instancia = new Trabajos();
        return instancia;
    }

    @Override
    public void comenzar() {
        mongoDb.establecerConexion();
        coleccionTrabajos = mongoDb.getBD().getCollection(COLECCION);
    }

    @Override
    public void terminar() {
        mongoDb.cerrarConexion();
    }

    private Trabajo getTrabajo(Document doc) {
        if (doc == null) return null;

        Cliente c = Clientes.getInstancia().buscar(Cliente.get(doc.getString(DNI_CLIENTE)));
        Vehiculo v = Vehiculos.getInstancia().buscar(Vehiculo.get(doc.getString(MATRICULA_VEHICULO)));
        LocalDate fInicio = LocalDate.parse(doc.getString(FECHA_INICIO), FORMATO_FECHA);

        Trabajo trabajo;
        if ("Revision".equals(doc.getString(TIPO))) {
            trabajo = new Revision(c, v, fInicio);
        } else {
            trabajo = new Mecanico(c, v, fInicio);
        }

        if (doc.getString(FECHA_FIN) != null) {
            try { trabajo.cerrar(LocalDate.parse(doc.getString(FECHA_FIN), FORMATO_FECHA)); } catch(Exception ignored){}
        }
        if (doc.getInteger(HORAS) != null) {
            try { trabajo.anadirHoras(doc.getInteger(HORAS)); } catch(Exception ignored){}
        }



        if (trabajo instanceof Mecanico mecanico && doc.get(PRECIO_MATERIAL) != null) {
            try { mecanico.anadirPrecioMaterial(doc.getDouble(PRECIO_MATERIAL).floatValue()); } catch(Exception ignored){}
        }
        return trabajo;
    }

    private Document getDocumento(Trabajo trabajo) {
        Document doc = new Document()
                .append(DNI_CLIENTE, trabajo.getCliente().getDni())
                .append(MATRICULA_VEHICULO, trabajo.getVehiculo().matricula())
                .append(FECHA_INICIO, trabajo.getFechaInicio().format(FORMATO_FECHA))
                .append(HORAS, trabajo.getHoras());

        if (trabajo instanceof Revision) {
            doc.append(TIPO, "Revision");
        } else if (trabajo instanceof Mecanico mecanico) {
            doc.append(TIPO, "Mecanico");
            doc.append(PRECIO_MATERIAL, (double) mecanico.getPrecioMaterial());
        }

        if (trabajo.getFechaFin() != null) {
            doc.append(FECHA_FIN, trabajo.getFechaFin().format(FORMATO_FECHA));
        }
        return doc;
    }

    @Override
    public List<Trabajo> get() {
        List<Trabajo> lista = new ArrayList<>();
        for (Document doc : coleccionTrabajos.find()) lista.add(getTrabajo(doc));
        return lista;
    }

    @Override
    public List<Trabajo> get(Cliente cliente) {
        List<Trabajo> lista = new ArrayList<>();
        for (Document doc : coleccionTrabajos.find(new Document(DNI_CLIENTE, cliente.getDni()))) lista.add(getTrabajo(doc));
        return lista;
    }

    @Override
    public List<Trabajo> get(Vehiculo vehiculo) {
        List<Trabajo> lista = new ArrayList<>();
        for (Document doc : coleccionTrabajos.find(new Document(MATRICULA_VEHICULO, vehiculo.matricula()))) lista.add(getTrabajo(doc));
        return lista;
    }

    @Override
    public Map<TipoTrabajo, Integer> getEstadisticasMensuales(LocalDate mes) {
        Map<TipoTrabajo, Integer> stats = new EnumMap<>(TipoTrabajo.class);
        for (TipoTrabajo tipo : TipoTrabajo.values()) stats.put(tipo, 0);
        for (Trabajo t : get()) {
            if (t.getFechaInicio().getMonth() == mes.getMonth() && t.getFechaInicio().getYear() == mes.getYear()) {
                TipoTrabajo tipo = (t instanceof Revision) ? TipoTrabajo.REVISION : TipoTrabajo.MECANICO;
                stats.put(tipo, stats.get(tipo) + 1);
            }
        }
        return stats;
    }

    @Override
    public void insertar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        if (trabajo == null) throw new NullPointerException("No se puede insertar un trabajo nulo.");
        if (buscar(trabajo) != null) throw new TallerMecanicoExcepcion("El trabajo ya existe.");
        coleccionTrabajos.insertOne(getDocumento(trabajo));
    }

    @Override
    public Trabajo anadirHoras(Trabajo trabajo, int horas) throws TallerMecanicoExcepcion {
        Trabajo t = buscar(trabajo);
        if (t == null) throw new TallerMecanicoExcepcion("El trabajo no existe.");
        t.anadirHoras(horas);
        coleccionTrabajos.updateOne(getFiltro(trabajo), new Document("$set", new Document(HORAS, t.getHoras())));
        return t;
    }

    @Override
    public Trabajo anadirPrecioMaterial(Trabajo trabajo, float precioMaterial) throws TallerMecanicoExcepcion {
        Trabajo t = buscar(trabajo);
        if (t == null) throw new TallerMecanicoExcepcion("El trabajo no existe.");

        if (t instanceof Mecanico mecanico) {
            mecanico.anadirPrecioMaterial(precioMaterial);
            coleccionTrabajos.updateOne(getFiltro(trabajo), new Document("$set", new Document(PRECIO_MATERIAL, (double) mecanico.getPrecioMaterial())));
            return mecanico;
        } else {
            throw new TallerMecanicoExcepcion("No se puede añadir precio de material a una revisión.");
        }
    }

    @Override
    public Trabajo cerrar(Trabajo trabajo, LocalDate fechaFin) throws TallerMecanicoExcepcion {
        Trabajo t = buscar(trabajo);
        if (t == null) throw new TallerMecanicoExcepcion("El trabajo no existe.");
        t.cerrar(fechaFin);
        coleccionTrabajos.updateOne(getFiltro(trabajo), new Document("$set", new Document(FECHA_FIN, t.getFechaFin().format(FORMATO_FECHA))));
        return t;
    }

    @Override
    public Trabajo buscar(Trabajo trabajo) {
        if (trabajo == null) return null;
        return getTrabajo(coleccionTrabajos.find(getFiltro(trabajo)).first());
    }

    @Override
    public void borrar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        if (trabajo == null) throw new NullPointerException("No se puede borrar un trabajo nulo.");
        if (buscar(trabajo) == null) throw new TallerMecanicoExcepcion("El trabajo no existe.");
        coleccionTrabajos.deleteOne(getFiltro(trabajo));
    }

    private Document getFiltro(Trabajo t) {
        return new Document(DNI_CLIENTE, t.getCliente().getDni())
                .append(MATRICULA_VEHICULO, t.getVehiculo().matricula())
                .append(FECHA_INICIO, t.getFechaInicio().format(FORMATO_FECHA));
    }
}