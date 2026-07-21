package com.example.mpct.api;

import com.example.mpct.model.entity.Configuracion;
import com.example.mpct.repository.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ConfiguracionRepository configuracionRepository;
    private final com.example.mpct.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private final com.example.mpct.repository.PagoRepository pagoRepository;
    private final com.example.mpct.repository.TramiteRepository tramiteRepository;
    private final com.example.mpct.service.InspeccionService inspeccionService;
    private final com.example.mpct.service.TramiteService tramiteService;
    private final com.example.mpct.service.AuthService authService;
    private final com.example.mpct.repository.CajaRepository cajaRepository;
    private final com.example.mpct.service.ComprobanteService comprobanteService;
    private final com.example.mpct.service.CajaService cajaService;

    @GetMapping("/configuraciones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Configuracion>> getAllConfiguraciones() {
        return ResponseEntity.ok(configuracionRepository.findAll());
    }

    @GetMapping("/pagos/pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<com.example.mpct.dto.pago.PagoResponse>> getPagosPendientes() {
        List<com.example.mpct.model.entity.Pago> pagos = pagoRepository.findByEstadoPago("PENDIENTE");
        List<com.example.mpct.dto.pago.PagoResponse> response = pagos.stream().map(p -> new com.example.mpct.dto.pago.PagoResponse(
                p.getId(), p.getTramite().getRuc(), p.getTramite().getRazonSocial(), p.getMonto(),
                p.getMetodoPago(), p.getEstadoPago(), p.getFechaPago()
        )).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/caja/cierres")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getReporteCierres() {
        
        List<com.example.mpct.model.entity.Caja> cajas = cajaRepository.findAll();
        cajas = cajas.stream()
                .filter(c -> c.getEstado() == com.example.mpct.model.enums.EstadoCaja.CERRADA)
                .sorted((a, b) -> b.getFechaCierre().compareTo(a.getFechaCierre()))
                .toList();

        List<java.util.Map<String, Object>> response = cajas.stream().map(c -> {
            java.math.BigDecimal declarado = c.getMontoDeclarado() != null ? c.getMontoDeclarado() : c.getMontoFinal();
            java.math.BigDecimal esperado = c.getMontoFinal();
            java.math.BigDecimal diferencia = declarado.subtract(esperado);
            
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("cajaId", c.getId());
            map.put("usuario", c.getUsuario().getEmail());
            map.put("fechaApertura", c.getFechaApertura().toString());
            map.put("fechaCierre", c.getFechaCierre() != null ? c.getFechaCierre().toString() : "");
            map.put("montoInicial", c.getMontoInicial());
            map.put("montoEsperado", esperado);
            map.put("montoDeclarado", declarado);
            map.put("diferencia", diferencia);
            
            return map;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pagos/{id}/voucher")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<byte[]> getVoucher(@PathVariable java.util.UUID id) {
        com.example.mpct.model.entity.Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
        if (pago.getArchivoVoucher() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                .body(pago.getArchivoVoucher());
    }

    @GetMapping("/pagos/pendientes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getPagosPendientes() {
        List<com.example.mpct.model.entity.Pago> pendientes = pagoRepository.findByEstadoPago("PENDIENTE");
        // Filtrar por estado del trámite para excluir trámites muertos/terminales
        pendientes = pendientes.stream()
                .filter(p -> p.getTramite().getEstado() == com.example.mpct.model.enums.EstadoTramite.PENDIENTE_PAGO)
                .sorted((a, b) -> b.getFechaPago().compareTo(a.getFechaPago()))
                .toList();
        
        List<java.util.Map<String, Object>> response = pendientes.stream().map(p -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("pagoId", p.getId());
            map.put("ruc", p.getTramite().getRuc());
            map.put("razonSocial", p.getTramite().getRazonSocial());
            map.put("monto", p.getMonto());
            map.put("metodoPago", p.getMetodoPago());
            map.put("numeroComprobante", p.getNumeroComprobante());
            map.put("fechaPago", p.getFechaPago());
            map.put("tramiteId", p.getTramite().getId());
            map.put("hasVoucher", p.getArchivoVoucher() != null);
            return map;
        }).toList();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pagos/{id}/validar")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> validarPago(
            @PathVariable java.util.UUID id, 
            @RequestBody ValidarPagoAdminRequest request,
            java.security.Principal principal
    ) {
        boolean aprobado = request.aprobado();
        String motivoOverride = request.motivoOverride();

        if (aprobado && (motivoOverride == null || motivoOverride.trim().isEmpty())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Debe especificar un motivoOverride para validar pagos administrativamente."));
        }

        com.example.mpct.model.entity.Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
        com.example.mpct.model.entity.Tramite tramite = pago.getTramite();

        if (aprobado) {
            pago.setEstadoPago("COMPLETADO");
            pago.setMotivoOverride(motivoOverride);
            pago.setValidadoPorAdmin(principal.getName());

            pago = pagoRepository.save(pago);
            if (tramite.getRequiereInspeccion()) {
                tramiteService.actualizarEstadoTramite(tramite, com.example.mpct.model.enums.EstadoTramite.PENDIENTE_REVISION, null);
            } else {
                tramiteService.actualizarEstadoTramite(tramite, com.example.mpct.model.enums.EstadoTramite.PAGADO, null);
                inspeccionService.programarInspeccionInicial(tramite);
            }
            
            // Generar Comprobante interno
            comprobanteService.generarYGuardar(pago);
            
            return ResponseEntity.ok(new MessageResponse("Pago aprobado por override. " + (tramite.getRequiereInspeccion() ? "Trámite pendiente de revisión." : "Trámite pagado.")));
        } else {
            pago.setEstadoPago("RECHAZADO");
            pago.setMotivoOverride(motivoOverride);
            pago.setValidadoPorAdmin(principal.getName());
            pagoRepository.save(pago);
            tramiteService.actualizarEstadoTramite(tramite, com.example.mpct.model.enums.EstadoTramite.PENDIENTE_PAGO, null);
            return ResponseEntity.ok(new MessageResponse("Pago rechazado. Trámite devuelto a pendiente de pago."));
        }
    }

    @PostMapping("/tramites/{ruc}/aprobar-revision")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSPECTOR')")
    public ResponseEntity<?> aprobarTramiteRevision(@PathVariable String ruc) {
        // En un flujo real, quizás el inspector lo aprueba.
        // Aquí llamamos al servicio que cambia el estado y emite la licencia.
        return ResponseEntity.ok(tramiteService.aprobarTramiteRevision(ruc));
    }

    @GetMapping("/tramites/revision")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSPECTOR')")
    public ResponseEntity<?> getTramitesEnRevision() {
        return ResponseEntity.ok(tramiteRepository.findByEstado(com.example.mpct.model.enums.EstadoTramite.PENDIENTE_REVISION));
    }

    @PutMapping("/configuraciones/{clave}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Configuracion> updatePrecio(
            @PathVariable String clave,
            @RequestParam("valor") BigDecimal valor
    ) {
        Configuracion conf = configuracionRepository.findByClave(clave)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada"));
        
        conf.setValor(valor);
        return ResponseEntity.ok(configuracionRepository.save(conf));
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changePassword(@jakarta.validation.Valid @RequestBody com.example.mpct.dto.auth.ChangePasswordRequest request, java.security.Principal principal) {
        try {
            authService.changePassword(principal.getName(), request);
            return ResponseEntity.ok(new MessageResponse("Contraseña actualizada exitosamente."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@jakarta.validation.Valid @RequestBody com.example.mpct.dto.auth.CreateUserRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse("El correo electrónico ya está en uso."));
        }

        if (request.role() == com.example.mpct.model.enums.Role.ADMIN) {
            return ResponseEntity.badRequest().body(new MessageResponse("Solo se pueden crear cuentas de INSPECTOR o CAJERO por esta vía."));
        }

        com.example.mpct.model.entity.User user = com.example.mpct.model.entity.User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .isActive(true)
                .build();

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Usuario registrado exitosamente."));
    }

    @PostMapping("/caja/{id}/forzar-cierre")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> forzarCierreCaja(
            @PathVariable java.util.UUID id,
            @jakarta.validation.Valid @RequestBody com.example.mpct.dto.caja.ForzarCierreRequest request,
            java.security.Principal principal
    ) {
        try {
            cajaService.forzarCierreCaja(id, request, principal.getName());
            return ResponseEntity.ok(new MessageResponse("Caja cerrada exitosamente por el administrador."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        List<com.example.mpct.model.entity.User> users = userRepository.findAll();
        List<java.util.Map<String, Object>> response = users.stream().map(u -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", u.getId());
            map.put("email", u.getEmail());
            map.put("role", u.getRole().name());
            map.put("isActive", u.getIsActive());
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).toList();
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/usuarios/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cambiarEstadoUsuario(
            @PathVariable java.util.UUID id,
            @RequestBody UsuarioEstadoRequest request,
            java.security.Principal principal
    ) {
        com.example.mpct.model.entity.User adminActual = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));
                
        if (id.equals(adminActual.getId())) {
            return ResponseEntity.badRequest().body(new MessageResponse("No puedes suspender tu propia cuenta"));
        }
        
        com.example.mpct.model.entity.User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        boolean newStatus = request.isActive();
        if (!newStatus && (request.motivoSuspension() == null || request.motivoSuspension().trim().isEmpty())) {
            return ResponseEntity.badRequest().body(new MessageResponse("El motivo de suspensión es obligatorio."));
        }
        
        user.setIsActive(newStatus);
        
        if (!newStatus) {
            user.setMotivoSuspension(request.motivoSuspension().trim());
            user.setSuspendidoPorAdmin(adminActual.getId().toString());
            user.setFechaSuspension(java.time.LocalDateTime.now());
        } else {
            user.setMotivoSuspension(null);
            user.setSuspendidoPorAdmin(null);
            user.setFechaSuspension(null);
        }
        
        userRepository.save(user);
        
        return ResponseEntity.ok(new MessageResponse(newStatus ? "Usuario reactivado exitosamente" : "Usuario suspendido exitosamente"));
    }
    
    public record ValidarPagoAdminRequest(boolean aprobado, String motivoOverride) {}
    public record UsuarioEstadoRequest(boolean isActive, String motivoSuspension) {}
    public record MessageResponse(String message) {}
}
