package com.example.mpct;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GenerateMoreInspections {
    public static void main(String[] args) throws Exception {
        String sql = """
INSERT INTO tramites (id, ruc, razon_social, domicilio_fiscal, representante_legal, rubro, dni, email, area, tipo, estado, archivo_plano, observaciones_generales, archivos_observados, fecha_limite_subsanacion, monto_cobrado, created_at, updated_at, requiere_inspeccion) VALUES
('22222222-2222-2222-2222-000000000001', '20100049181', 'SUPERMERCADOS PERUANOS S.A. (PLAZA VEA)', 'Av. España 123', 'Juan Vallejo', 'SUPERMERCADO', '20000001', 'test@test.com', 5000.00, 'NUEVO', 'PROGRAMADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', true),
('22222222-2222-2222-2222-000000000002', '20504746686', 'MAESTRO PERU S.A.', 'Av. Mansiche S/N', 'Carlos Castro', 'FERRETERIA', '20000002', 'test@test.com', 8000.00, 'MODIFICACION', 'PROGRAMADO', lo_creat(-1), NULL, NULL, NULL, 120.00, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days', true),
('22222222-2222-2222-2222-000000000003', '20536557859', 'PROMART HOMECENTER', 'Real Plaza Trujillo', 'Julio Perez', 'FERRETERIA', '20000003', 'test@test.com', 10000.00, 'NUEVO', 'PROGRAMADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', true),
('22222222-2222-2222-2222-000000000004', '20100130972', 'CASSINELLI S.A.', 'Av. Nicolas de Pierola 1100', 'Jose Cassinelli', 'ACABADOS', '20000004', 'test@test.com', 4000.00, 'NUEVO', 'PROGRAMADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', true),
('22222222-2222-2222-2222-000000000005', '20511874289', 'REAL PLAZA TRUJILLO', 'Av. Cesar Vallejo Oeste', 'Rafael Dasso', 'CENTRO COMERCIAL', '20000005', 'test@test.com', 40000.00, 'RENOVACION', 'PROGRAMADO', lo_creat(-1), NULL, NULL, NULL, 90.00, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days', true),
('22222222-2222-2222-2222-000000000006', '20513970668', 'MALL AVENTURA PLAZA', 'Av. Mansiche', 'Mauricio Mendoza', 'CENTRO COMERCIAL', '20000006', 'test@test.com', 45000.00, 'NUEVO', 'PROGRAMADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', true),
('22222222-2222-2222-2222-000000000007', '20141703163', 'COLEGIO CLARETIANO', 'Urb. San Andres', 'Padre Claret', 'COLEGIO', '20000007', 'test@test.com', 6000.00, 'NUEVO', 'PROGRAMADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days', true),
('22222222-2222-2222-2222-000000000008', '20141549491', 'COLEGIO SAN JOSE OBRERO', 'Los Pinos', 'Padre Jorge', 'COLEGIO', '20000008', 'test@test.com', 5000.00, 'NUEVO', 'PROGRAMADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '1 days', NOW() - INTERVAL '1 days', true),
('22222222-2222-2222-2222-000000000009', '20429683582', 'CINEPLANET', 'Real Plaza Trujillo', 'Fernando Soriano', 'CINE', '20000009', 'test@test.com', 3000.00, 'MODIFICACION', 'PROGRAMADO', lo_creat(-1), NULL, NULL, NULL, 120.00, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', true),
('22222222-2222-2222-2222-000000000010', '20131489628', 'SQUALOS RESTAURANTE', 'Jr. Pizarro 231', 'Carlos Squalo', 'RESTAURANTE', '20000010', 'test@test.com', 400.00, 'NUEVO', 'PROGRAMADO', lo_creat(-1), NULL, NULL, NULL, 180.00, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', true);

INSERT INTO pagos (id, tramite_id, monto, metodo_pago, estado_pago, archivo_voucher, numero_comprobante, pasarela_transaction_id, fecha_pago) VALUES
(gen_random_uuid(), '22222222-2222-2222-2222-000000000001', 180.00, 'MERCADOPAGO', 'APROBADO', NULL, NULL, 'MP-2001', NOW()),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000002', 120.00, 'MERCADOPAGO', 'APROBADO', NULL, NULL, 'MP-2002', NOW()),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000003', 180.00, 'BANCO_NACION', 'APROBADO', lo_creat(-1), 'BN-2003', NULL, NOW()),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000004', 180.00, 'BANCO_NACION', 'APROBADO', lo_creat(-1), 'BN-2004', NULL, NOW()),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000005', 90.00, 'MERCADOPAGO', 'APROBADO', NULL, NULL, 'MP-2005', NOW()),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000006', 180.00, 'BANCO_NACION', 'APROBADO', lo_creat(-1), 'BN-2006', NULL, NOW()),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000007', 180.00, 'MERCADOPAGO', 'APROBADO', NULL, NULL, 'MP-2007', NOW()),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000008', 180.00, 'MERCADOPAGO', 'APROBADO', NULL, NULL, 'MP-2008', NOW()),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000009', 120.00, 'BANCO_NACION', 'APROBADO', lo_creat(-1), 'BN-2009', NULL, NOW()),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000010', 180.00, 'MERCADOPAGO', 'APROBADO', NULL, NULL, 'MP-2010', NOW());

INSERT INTO inspecciones (id, tramite_id, inspector_id, numero_inspeccion, estado, observaciones, fecha_programada, fecha_realizada) VALUES
(gen_random_uuid(), '22222222-2222-2222-2222-000000000001', NULL, 1, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '10 hours', NULL),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000002', NULL, 1, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '12 hours', NULL),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000003', NULL, 1, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '1 days' + INTERVAL '10 hours', NULL),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000004', NULL, 1, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '1 days' + INTERVAL '14 hours', NULL),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000005', NULL, 1, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '2 days' + INTERVAL '9 hours', NULL),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000006', NULL, 1, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '2 days' + INTERVAL '16 hours', NULL),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000007', NULL, 1, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '3 days' + INTERVAL '11 hours', NULL),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000008', NULL, 1, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '3 days' + INTERVAL '15 hours', NULL),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000009', NULL, 1, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '4 days' + INTERVAL '10 hours', NULL),
(gen_random_uuid(), '22222222-2222-2222-2222-000000000010', NULL, 1, 'PROGRAMADA', NULL, CURRENT_DATE + INTERVAL '4 days' + INTERVAL '14 hours', NULL);
""";
        Files.write(Paths.get("C:\\Users\\Blae\\.gemini\\antigravity-cli\\brain\\52b12342-8b88-4ad3-9e13-13ef9e1aa020\\test_data_more.sql"), sql.getBytes());
    }
}
