package com.agora.admin.service;

import com.agora.admin.model.AdminStats;
import com.agora.admin.model.CountryCount;
import com.agora.admin.repository.AdminStatsRepository;
import com.agora.admin.repository.CountryCountRepository;
import com.agora.auth.model.RoleEnum;
import com.agora.user.model.User;
import com.agora.user.repository.UserRepository;
import com.agora.user.repository.RoleRepository;
import com.agora.event.repository.EventRepository;
import com.agora.event.model.Event;
import com.agora.event.dto.EventDTO;
import com.agora.event.service.EventService;
import com.agora.event.model.EventType;
import com.agora.auth.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminStatsRepository adminStatsRepository;

    @Autowired
    private CountryCountRepository countryCountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private com.agora.user.repository.UserReportRepository userReportRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventService eventService;

    @Transactional
    public AdminStats refreshStats() {
        long total = userRepository.count();
        long investigators = userRepository.countByRolesRoleName(RoleEnum.ACADEMICO);
        long communicators = userRepository.countByRolesRoleName(RoleEnum.COMUNICADOR);

        AdminStats stats = new AdminStats(total, investigators, communicators);
        adminStatsRepository.save(stats);

        // refresh country counts (top 8)
        List<Object[]> byCountry = userRepository.countUsersByCountry();
        List<CountryCount> countries = new ArrayList<>();
        int limit = Math.min(8, byCountry.size());
        for (int i = 0; i < limit; i++) {
            Object[] row = byCountry.get(i);
            String country = (String) row[0];
            Number cnt = (Number) row[1];
            countries.add(new CountryCount(country, cnt.longValue()));
        }

        // replace existing country counts
        countryCountRepository.deleteAll();
        countryCountRepository.saveAll(countries);

        return stats;
    }

    public AdminStats getLatestStats() {
        return adminStatsRepository.findTopByOrderByCreatedAtDesc().orElse(null);
    }

    public List<CountryCount> getTopCountries() {
        return countryCountRepository.findAll();
    }

    // --- Users management ---
    public List<com.agora.user.model.User> getAllUsers() {
        return userRepository.findAll();
    }

    public com.agora.user.model.User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public long getUsersNumber() {
        return userRepository.count();
    }

    public java.util.Map<String, Long> getUsersPerRole() {
        long investigators = userRepository.countByRolesRoleName(RoleEnum.ACADEMICO);
        long communicators = userRepository.countByRolesRoleName(RoleEnum.COMUNICADOR);
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        map.put("investigadores", investigators);
        map.put("comunicadores", communicators);
        return map;
    }

    public java.util.Map<String, Long> getUsersPerCountry() {
        java.util.List<Object[]> rows = userRepository.countUsersByCountry();
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        // ensure keys for requested countries
        String[] expected = {"chile", "argentina", "peru", "mexico", "colombia"};
        for (String c : expected) map.put(c, 0L);

        for (Object[] row : rows) {
            if (row == null || row.length < 2) continue;
            String country = ((String) row[0]).toLowerCase();
            Number cnt = (Number) row[1];
            if (country == null) continue;
            map.put(country, cnt.longValue());
        }

        return map;
    }

    @Transactional
    public com.agora.user.model.User banUser(Long id, Boolean enabled) {
        com.agora.user.model.User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (enabled != null) user.setIsEnabled(enabled);
        else user.setIsEnabled(!Boolean.TRUE.equals(user.getIsEnabled()));
        return userRepository.save(user);
    }

    @Transactional
    public User resetReports(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Se borran los reportes para que el contador vuelva a empezar de cero
        userReportRepository.deleteByReportedId(id);
        u.setReportCount(0);
        u.setIsEnabled(true);

        return userRepository.save(u);
    }
    @Transactional
    public com.agora.user.model.User changeUserRole(Long id, RoleEnum targetRole) {
        com.agora.user.model.User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByRoleName(targetRole)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        java.util.Set<Role> roles = new java.util.HashSet<>();
        roles.add(role);
        user.setRoles((java.util.Set) roles);
        return userRepository.save(user);
    }

    // --- Events management ---
    public java.util.List<EventDTO> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(eventService::toDTO)
                .toList();
    }

    public EventDTO getEventById(Long id) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return eventService.toDTO(e);
    }

    public long getEventsNumber() {
        return eventRepository.count();
    }

    public java.util.Map<String, Long> getEventsPerType() {
        java.util.List<Object[]> rows = eventRepository.countEventsByType();
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        // initialize expected keys
        map.put("charlas", 0L);
        map.put("concursos", 0L);
        map.put("congresos", 0L);
        map.put("conferencias", 0L);

        for (Object[] r : rows) {
            if (r == null || r.length < 2) continue;
            EventType type = (EventType) r[0];
            Number cnt = (Number) r[1];
            String key = switch (type) {
                case CHARLA -> "charlas";
                case CONCURSO -> "concursos";
                case CONGRESO -> "congresos";
                case CONFERENCIA -> "conferencias";
            };
            map.put(key, cnt.longValue());
        }
        return map;
    }

    @Transactional
    public EventDTO banEvent(Long id, Boolean enabled) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if (enabled != null) event.setIsEnabled(enabled);
        else event.setIsEnabled(!Boolean.TRUE.equals(event.getIsEnabled()));
        Event saved = eventRepository.save(event);
        return eventService.toDTO(saved);
    }

    @Transactional
    public EventDTO changeEventType(Long id, EventType newType) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        event.setType(newType);
        Event saved = eventRepository.save(event);
        return eventService.toDTO(saved);
    }
}
