package org.iesalandalus.programacion.tallermecanico.modelo.negocio.ficheros.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.Vehiculo;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.IVehiculos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Vehiculos implements IVehiculos {

    private static final String FICHERO_VEHICULOS = String.format("%s%s%s%s%s%s%s", "datos", File.separator, "ficheros", File.separator, "json", File.separator, "vehiculos.json");

    private static Vehiculos instancia;
    private final ObjectMapper mapper;

    private final List<Vehiculo> coleccionVehiculos;

    private Vehiculos() {
        coleccionVehiculos = new ArrayList<>();
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    static Vehiculos getInstancia() {
        if (instancia == null) {
            instancia = new Vehiculos();
        }
        return instancia;
    }

    @Override
    public void comenzar() {
        File fichero = new File(FICHERO_VEHICULOS);
        if (fichero.exists()) {
            try {
                List<Vehiculo> vehiculosLeidos = mapper.readValue(fichero, new TypeReference<List<Vehiculo>>() {});
                coleccionVehiculos.clear();
                coleccionVehiculos.addAll(vehiculosLeidos);
                System.out.printf("Fichero %s leído correctamente.%n", FICHERO_VEHICULOS);
            } catch (IOException e) {
                System.out.printf("No se ha podido leer el fichero %s.%n", FICHERO_VEHICULOS);
            }
        }
    }

    @Override
    public void terminar() {
        File fichero = new File(FICHERO_VEHICULOS);
        try {
            mapper.writeValue(fichero, coleccionVehiculos);
            System.out.printf("Fichero %s escrito correctamente.%n", FICHERO_VEHICULOS);
        } catch (IOException e) {
            System.out.printf("No se ha podido escribir el fichero %s.%n", FICHERO_VEHICULOS);
        }
    }

    @Override
    public List<Vehiculo> get() {
        return new ArrayList<>(coleccionVehiculos);
    }

    @Override
    public void insertar(Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(vehiculo, "No se puede insertar un vehículo nulo.");
        if (coleccionVehiculos.contains(vehiculo)) {
            throw new TallerMecanicoExcepcion("Ya existe un vehículo con esa matrícula.");
        }
        coleccionVehiculos.add(vehiculo);
    }

    @Override
    public Vehiculo buscar(Vehiculo vehiculo) {
        Objects.requireNonNull(vehiculo, "No se puede buscar un vehículo nulo.");
        int indice = coleccionVehiculos.indexOf(vehiculo);
        return (indice == -1) ? null : coleccionVehiculos.get(indice);
    }

    @Override
    public void borrar(Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(vehiculo, "No se puede borrar un vehículo nulo.");
        if (!coleccionVehiculos.contains(vehiculo)) {
            throw new TallerMecanicoExcepcion("No existe ningún vehículo con esa matrícula.");
        }
        coleccionVehiculos.remove(vehiculo);
    }
}