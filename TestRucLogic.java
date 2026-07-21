import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class TestRucLogic {
    public static void main(String[] args) {
        testA();
        testB();
        testC();
    }

    private static Map<String, String> getMejorGerente(List<Map<String, String>> representantes) {
        Map<String, String> gerente = null;
        if (representantes != null && !representantes.isEmpty()) {
            List<String> cargosPrioridad = List.of(
                    "GERENTE GENERAL", "GERENTE", "APODERADO", "TITULAR-GERENTE",
                    "PRESIDENTE DE DIRECTORIO", "DIRECTOR GERENTE"
            );

            int mejorPrioridadIndex = Integer.MAX_VALUE;
            LocalDate mejorFecha = null;

            for (Map<String, String> rep : representantes) {
                String cargoStr = rep.get("cargo");
                if (cargoStr == null) continue;
                String cargoNormalizado = cargoStr.trim().toUpperCase();

                for (int i = 0; i < cargosPrioridad.size(); i++) {
                    if (cargoNormalizado.contains(cargosPrioridad.get(i))) {
                        String fechaStr = rep.get("fecha_desde");
                        LocalDate fechaDesde = null;
                        if (fechaStr != null && !fechaStr.trim().isEmpty()) {
                            try {
                                fechaDesde = LocalDate.parse(fechaStr.trim());
                            } catch (Exception ignored) {
                            }
                        }

                        if (i < mejorPrioridadIndex) {
                            mejorPrioridadIndex = i;
                            gerente = rep;
                            mejorFecha = fechaDesde;
                        } else if (i == mejorPrioridadIndex) {
                            if (fechaDesde != null && (mejorFecha == null || fechaDesde.isBefore(mejorFecha))) {
                                gerente = rep;
                                mejorFecha = fechaDesde;
                            }
                        }
                        break;
                    }
                }
            }
        }
        return gerente;
    }

    private static void testA() {
        System.out.println("Test A: GERENTE GENERAL over APODERADO");
        List<Map<String, String>> reps = List.of(
            Map.of("cargo", "APODERADO", "nombre", "JUAN APODERADO", "fecha_desde", "2020-01-01"),
            Map.of("cargo", "GERENTE GENERAL", "nombre", "MARIA GERENTE", "fecha_desde", "2023-01-01")
        );
        Map<String, String> result = getMejorGerente(reps);
        System.out.println("Elegido: " + (result != null ? result.get("nombre") + " - " + result.get("cargo") : "null"));
        System.out.println();
    }

    private static void testB() {
        System.out.println("Test B: Tie-breaker with oldest fecha_desde");
        List<Map<String, String>> reps = List.of(
            Map.of("cargo", "APODERADO", "nombre", "PEDRO RECIENTE", "fecha_desde", "2023-01-01"),
            Map.of("cargo", "APODERADO COMERCIAL", "nombre", "JUAN ANTIGUO", "fecha_desde", "2015-01-01")
        );
        Map<String, String> result = getMejorGerente(reps);
        System.out.println("Elegido: " + (result != null ? result.get("nombre") + " - " + result.get("cargo") + " (" + result.get("fecha_desde") + ")" : "null"));
        System.out.println();
    }

    private static void testC() {
        System.out.println("Test C: No valid role leaves empty");
        List<Map<String, String>> reps = List.of(
            Map.of("cargo", "CONTADOR PUBLICO", "nombre", "ALEX CONTADOR"),
            Map.of("cargo", "SECRETARIA", "nombre", "ANA SECRETARIA")
        );
        Map<String, String> result = getMejorGerente(reps);
        System.out.println("Elegido: " + (result != null ? result.get("nombre") : "null"));
    }
}
