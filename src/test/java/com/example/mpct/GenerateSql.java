import java.nio.file.Files;
import java.nio.file.Paths;

public class GenerateSql {
    public static void main(String[] args) throws Exception {
        String sql = """
-- 1. Limpieza
DELETE FROM pagos;
DELETE FROM licencias;
DELETE FROM inspecciones;
DELETE FROM tramites;

-- 2. Aseguramos columnas
ALTER TABLE licencias ADD COLUMN IF NOT EXISTS contador_notificaciones INT DEFAULT 0;
ALTER TABLE licencias ADD COLUMN IF NOT EXISTS ultima_notificacion_renovacion TIMESTAMP;

-- 3. Insertamos Tramites
INSERT INTO tramites (id, ruc, razon_social, domicilio_fiscal, representante_legal, rubro, dni, email, area, tipo, estado, archivo_plano, observaciones_generales, archivos_observados, fecha_limite_subsanacion, monto_cobrado, created_at, updated_at, requiere_inspeccion) VALUES
('11111111-1111-1111-1111-000000000001', '20141878477', 'UNIVERSIDAD PRIVADA ANTENOR ORREGO', 'Av. America Sur 3145', 'Felicita Peralta', 'EDUCACION', '10000001', 'test@test.com', 25000.00, 'NUEVO', 'APROBADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '1 year', NOW() - INTERVAL '1 year', false),
('11111111-1111-1111-1111-000000000002', '20131102994', 'CAJA MUNICIPAL TRUJILLO', 'Jr. Pizarro 414', 'Carlos Vives', 'FINANZAS', '10000002', 'test@test.com', 850.00, 'NUEVO', 'APROBADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '362 days', NOW() - INTERVAL '362 days', false),
('11111111-1111-1111-1111-000000000003', '20132104206', 'SEDALIB', 'Los Sapos 415', 'Juan Carlos', 'SANEAMIENTO', '10000003', 'test@test.com', 12000.00, 'NUEVO', 'APROBADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '364 days', NOW() - INTERVAL '364 days', false),
('11111111-1111-1111-1111-000000000004', '20131920721', 'DANPER TRUJILLO', 'Carretera Industrial', 'Jorge Salazar', 'AGROINDUSTRIA', '10000004', 'test@test.com', 45000.00, 'NUEVO', 'APROBADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '6 months', NOW() - INTERVAL '6 months', false),
('11111111-1111-1111-1111-000000000005', '20131495007', 'TRANSPORTES LINEA', 'Av. America Sur', 'Pedro Perez', 'TRANSPORTES', '10000005', 'test@test.com', 5000.00, 'NUEVO', 'PENDIENTE_PAGO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', true),
('11111111-1111-1111-1111-000000000006', '20531535775', 'ITTSA', 'Av. Mansiche', 'Luis Gomez', 'TRANSPORTES', '10000006', 'test@test.com', 4000.00, 'RENOVACION', 'VALIDANDO_PAGO', lo_creat(-1), NULL, NULL, NULL, 90.00, NOW() - INTERVAL '1 days', NOW() - INTERVAL '1 days', true),
('11111111-1111-1111-1111-000000000007', '20132002538', 'GOLF Y COUNTRY CLUB', 'Urb. El Golf', 'Manuel Cerna', 'CLUB', '10000007', 'test@test.com', 30000.00, 'NUEVO', 'PROGRAMADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', true),
('11111111-1111-1111-1111-000000000008', '20141680103', 'UNIVERSIDAD NACIONAL', 'Av. Juan Pablo II', 'Rector', 'EDUCACION', '10000008', 'test@test.com', 40000.00, 'NUEVO', 'OBSERVADO', lo_creat(-1), 'Faltan extintores', 'FOTO', NOW() + INTERVAL '30 days', 180.00, NOW() - INTERVAL '10 days', NOW() - INTERVAL '1 days', true),
('11111111-1111-1111-1111-000000000009', '20480373204', 'CLINICA PERUANO AMERICANA', 'Av. Mansiche', 'Rosa Ruiz', 'SALUD', '10000009', 'test@test.com', 2000.00, 'NUEVO', 'RECHAZADO', lo_creat(-1), 'No levantó observaciones', 'INFORME', NULL, 180.00, NOW() - INTERVAL '40 days', NOW() - INTERVAL '1 days', true);

INSERT INTO pagos (id, tramite_id, monto, metodo_pago, estado_pago, archivo_voucher, numero_comprobante, pasarela_transaction_id, fecha_pago) VALUES
(gen_random_uuid(), '11111111-1111-1111-1111-000000000001', 180.00, 'MERCADOPAGO', 'APROBADO', NULL, NULL, 'MP-1001', NOW()),
(gen_random_uuid(), '11111111-1111-1111-1111-000000000002', 180.00, 'BANCO_NACION', 'APROBADO', lo_creat(-1), 'BN-2002', NULL, NOW()),
(gen_random_uuid(), '11111111-1111-1111-1111-000000000003', 180.00, 'MERCADOPAGO', 'APROBADO', NULL, NULL, 'MP-1003', NOW()),
(gen_random_uuid(), '11111111-1111-1111-1111-000000000004', 180.00, 'BANCO_NACION', 'APROBADO', lo_creat(-1), 'BN-2004', NULL, NOW()),
(gen_random_uuid(), '11111111-1111-1111-1111-000000000006', 90.00, 'BANCO_NACION', 'PENDIENTE', lo_creat(-1), 'BN-9999', NULL, NOW()),
(gen_random_uuid(), '11111111-1111-1111-1111-000000000007', 180.00, 'MERCADOPAGO', 'APROBADO', NULL, NULL, 'MP-1007', NOW()),
(gen_random_uuid(), '11111111-1111-1111-1111-000000000008', 180.00, 'MERCADOPAGO', 'APROBADO', NULL, NULL, 'MP-1008', NOW()),
(gen_random_uuid(), '11111111-1111-1111-1111-000000000009', 180.00, 'BANCO_NACION', 'APROBADO', lo_creat(-1), 'BN-2009', NULL, NOW());

INSERT INTO licencias (id, tramite_id, numero_licencia, qr_data, pdf_archivo, fecha_emision, fecha_vencimiento, codigo_catastral, estado, contador_notificaciones, ultima_notificacion_renovacion) VALUES
(gen_random_uuid(), '11111111-1111-1111-1111-000000000001', 'LIC-2025-0001', 'UPAO', lo_creat(-1), CURRENT_DATE - INTERVAL '1 year', CURRENT_DATE - INTERVAL '1 day', 'CAT-001', 'VENCIDA', 0, NULL),
(gen_random_uuid(), '11111111-1111-1111-1111-000000000002', 'LIC-2025-0002', 'CAJA', lo_creat(-1), CURRENT_DATE - INTERVAL '362 days', CURRENT_DATE + INTERVAL '3 days', 'CAT-002', 'VIGENTE', 0, NULL),
(gen_random_uuid(), '11111111-1111-1111-1111-000000000003', 'LIC-2025-0003', 'SEDALIB', lo_creat(-1), CURRENT_DATE - INTERVAL '364 days', CURRENT_DATE + INTERVAL '1 day', 'CAT-003', 'VIGENTE', 0, NULL),
(gen_random_uuid(), '11111111-1111-1111-1111-000000000004', 'LIC-2025-0004', 'DANPER', lo_creat(-1), CURRENT_DATE - INTERVAL '6 months', CURRENT_DATE + INTERVAL '6 months', 'CAT-004', 'VIGENTE', 0, NULL);

INSERT INTO inspecciones (id, tramite_id, inspector_id, numero_inspeccion, estado, observaciones, fecha_programada, fecha_realizada) VALUES
(gen_random_uuid(), '11111111-1111-1111-1111-000000000007', NULL, 1, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '10 hours', NULL),
(gen_random_uuid(), '11111111-1111-1111-1111-000000000008', NULL, 2, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '20 days', NULL);
""";
        Files.write(Paths.get("C:\\Users\\Blae\\.gemini\\antigravity-cli\\brain\\52b12342-8b88-4ad3-9e13-13ef9e1aa020\\test_data.sql"), sql.getBytes());
    }
}
