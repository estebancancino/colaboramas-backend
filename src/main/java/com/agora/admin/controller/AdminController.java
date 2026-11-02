package com.agora.admin.controller;

import com.agora.admin.model.AdminStats;
import com.agora.admin.model.CountryCount;
import com.agora.admin.service.AdminService;
import com.agora.admin.dto.ChangeRoleRequest;
import com.agora.admin.dto.ChangeEventTypeRequest;
// returning DTOs for events to avoid serializing JPA proxies
import com.agora.user.model.User;
import com.agora.event.model.EventType;
import com.agora.auth.model.RoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/refresh-stats")
    public ResponseEntity<AdminStats> refreshStats() {
        AdminStats stats = adminService.refreshStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStats> getLatestStats() {
        AdminStats stats = adminService.getLatestStats();
        if (stats == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/countries")
    public ResponseEntity<List<CountryCount>> getTopCountries() {
        return ResponseEntity.ok(adminService.getTopCountries());
    }

    // ==================== USER MANAGEMENT ENDPOINTS ====================
    
    // GET /admin/users - Get all users
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // GET /admin/users/{id} - Get user by ID
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    // GET /admin/usersNumber - Get total number of users
    @GetMapping("/usersNumber")
    public ResponseEntity<java.util.Map<String, Long>> getUsersNumber() {
        return ResponseEntity.ok(java.util.Map.of("total", adminService.getUsersNumber()));
    }

    // GET /admin/usersPerRol - Get users grouped by role
    @GetMapping("/usersPerRol")
    public ResponseEntity<java.util.Map<String, Long>> getUsersPerRol() {
        return ResponseEntity.ok(adminService.getUsersPerRole());
    }

    // GET /admin/userPerCountry - Get users grouped by country
    @GetMapping("/userPerCountry")
    public ResponseEntity<java.util.Map<String, Long>> getUsersPerCountry() {
        return ResponseEntity.ok(adminService.getUsersPerCountry());
    }

    // PATCH /admin/users/{id}/role - Change user role (query param version)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<User> changeUserRole(@PathVariable Long id, @RequestParam("role") RoleEnum role) {
        return ResponseEntity.ok(adminService.changeUserRole(id, role));
    }

    // PATCH /admin/changeUserRole/{id} - Change user role (accepts both query param and body)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/changeUserRole/{id}")
    public ResponseEntity<User> changeUserRoleAlt(
            @PathVariable Long id, 
            @RequestParam(value = "role", required = false) RoleEnum roleParam,
            @RequestBody(required = false) ChangeRoleRequest roleBody) {
        
        // Accept role from either query param or request body
        RoleEnum role = roleParam != null ? roleParam : (roleBody != null ? roleBody.getRole() : null);
        
        if (role == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(adminService.changeUserRole(id, role));
    }

    // PATCH /admin/users/{id}/ban - Ban/unban user
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<User> banUser(@PathVariable Long id, @RequestParam(name = "enabled", required = false) Boolean enabled) {
        User u = adminService.banUser(id, enabled);
        return ResponseEntity.ok(u);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/resetReports")
    public ResponseEntity<User> resetReports(@PathVariable Long id) {
        User u = adminService.resetReports(id);
        return ResponseEntity.ok(u);
    }
    // ==================== EVENT MANAGEMENT ENDPOINTS ====================
    
    // GET /admin/events - Get all events
    @GetMapping("/events")
    public ResponseEntity<List<com.agora.event.dto.EventDTO>> getAllEvents() {
        return ResponseEntity.ok(adminService.getAllEvents());
    }

    // GET /admin/events/{id} - Get event by ID
    @GetMapping("/events/{id}")
    public ResponseEntity<com.agora.event.dto.EventDTO> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getEventById(id));
    }

    // GET /admin/eventsNumber - Get total number of events
    @GetMapping("/eventsNumber")
    public ResponseEntity<java.util.Map<String, Long>> getEventsNumber() {
        return ResponseEntity.ok(java.util.Map.of("total", adminService.getEventsNumber()));
    }

    // GET /admin/eventsPerType - Get events grouped by type
    @GetMapping("/eventsPerType")
    public ResponseEntity<java.util.Map<String, Long>> getEventsPerType() {
        return ResponseEntity.ok(adminService.getEventsPerType());
    }

    // PATCH /admin/events/{id}/ban - Ban/unban event
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/events/{id}/ban")
    public ResponseEntity<com.agora.event.dto.EventDTO> banEvent(@PathVariable Long id, @RequestParam(name = "enabled", required = false) Boolean enabled) {
        return ResponseEntity.ok(adminService.banEvent(id, enabled));
    }

    // PATCH /admin/events/{id}/type - Change event type
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/events/{id}/type")
    public ResponseEntity<com.agora.event.dto.EventDTO> changeEventType(
            @PathVariable Long id,
            @RequestParam(value = "type", required = false) EventType typeParam,
            @RequestBody(required = false) ChangeEventTypeRequest typeBody) {

        EventType type = typeParam != null ? typeParam : (typeBody != null ? typeBody.getType() : null);
        if (type == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(adminService.changeEventType(id, type));
    }
}
