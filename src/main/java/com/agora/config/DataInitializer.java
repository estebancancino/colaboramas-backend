package com.agora.config;

import com.agora.auth.model.PermissionEntity;
import com.agora.auth.model.PermissionEnum;
import com.agora.auth.repository.PermissionRepository;
import com.agora.profile.model.Educacion;
import com.agora.tag.model.Keyword;
import com.agora.tag.model.KeywordType;
import com.agora.tag.repository.KeywordRepository;
import com.agora.event.model.Event;
import com.agora.event.model.EventType;
import com.agora.event.repository.EventRepository;
import com.agora.user.repository.RoleRepository;
import com.agora.user.repository.UserRepository;
import com.agora.auth.model.Role;
import com.agora.auth.model.RoleEnum;
import com.agora.user.model.User;
import com.agora.user.model.UserKeyword;
import org.slf4j.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            KeywordRepository keywordRepository,
            EventRepository eventRepository,
            Environment environment
    ) {
        return args -> {
            // Se leen al ejecutar (no con @Value) para que el .env de DotEnvConfig ya esté cargado
            String adminEmail = environment.getProperty("app.admin.email", "").trim();
            String adminPassword = environment.getProperty("app.admin.password", "");

            // --- 1. Creación de Permisos y Roles ---
            List<PermissionEntity> permissions = Arrays.stream(PermissionEnum.values())
                    .map(p -> PermissionEntity.builder().permissionName(p).build())
                    .toList();
            permissions.forEach(p ->
                    permissionRepository.findByPermissionName(p.getPermissionName())
                            .orElseGet(() -> permissionRepository.save(p))
            );
            var allPermissions = new HashSet<>(permissionRepository.findAll());

            Role academicoRole = roleRepository.findByRoleName(RoleEnum.ACADEMICO)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .roleName(RoleEnum.ACADEMICO)
                            .permissionEntities(allPermissions)
                            .build()));

            Role comunicadorRole = roleRepository.findByRoleName(RoleEnum.COMUNICADOR)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .roleName(RoleEnum.COMUNICADOR)
                            .permissionEntities(allPermissions)
                            .build()));

            Role adminRole = roleRepository.findByRoleName(RoleEnum.ADMIN)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .roleName(RoleEnum.ADMIN)
                            .permissionEntities(allPermissions)
                            .build()));

            // --- 2. Creación de Keywords Maestras (solo nombres) ---
            List<String> keywordNames = List.of(
                    "Inteligencia Artificial", "Machine Learning", "Biología Molecular",
                    "Sociología Urbana", "Energías Renovables", "Divulgación Científica",
                    "Desarrollo Sostenible", "Educación", "Arte y Tecnología"
            );
            keywordNames.forEach(name ->
                    keywordRepository.findByName(name)
                            .orElseGet(() -> keywordRepository.save(Keyword.builder().name(name).build()))
            );

            // --- 3. Creación de Usuarios de Prueba ---
            var encoder = new BCryptPasswordEncoder();

            // Usuario Admin: se crea desde ADMIN_EMAIL / ADMIN_PASSWORD, solo si no existe
            if (adminEmail.isBlank() || adminPassword.isBlank()) {
                logger.warn("ADMIN_EMAIL o ADMIN_PASSWORD no definidos: no se crea el usuario ADMIN inicial");
            } else if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = User.builder()
                        .username("Administrador")
                        .email(adminEmail)
                        .imageUrl("https://cdn-icons-png.flaticon.com/512/1177/1177568.png")
                        .password(encoder.encode(adminPassword))
                        .roles(Set.of(adminRole))
                        .pais("Argentina").ciudad("Buenos Aires")
                        .latitud(-34.6037).longitud(-58.3816)
                        .motivaciones("Gestión y administración de la plataforma Ágora.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                admin.addEducacion(Educacion.builder().institucion("Sistema").titulo("Administrador").build());
                userRepository.save(admin);
                logger.info("Usuario ADMIN inicial creado: {}", adminEmail);
            }

            // Usuario 1: Ana Gómez
            if (userRepository.findByEmail("ana.gomez@email.com").isEmpty()) {
                User ana = User.builder()
                        .username("Ana Gómez")
                        .email("ana.gomez@email.com")
                        .imageUrl("https://plus.unsplash.com/premium_photo-1688572454849-4348982edf7d?ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8MXx8cmV0cmF0byUyMGRlJTIwcGVyc29uYXxlbnwwfHwwfHx8MA%3D%3D&fm=jpg&q=60&w=3000")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("Argentina").ciudad("Buenos Aires")
                        .latitud(-34.6037).longitud(-58.3816)
                        .motivaciones("Busco colaborar en proyectos que apliquen IA para resolver problemas sociales.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                ana.addEducacion(Educacion.builder().institucion("UBA").titulo("Doctorado en Ciencias de la Computación").build());
                ana.setProyectosRecientes("Proyecto de automatizacion");
                ana.setActividadesPersonales("Jugadora de cartas");
                addKeywordsToUser(ana, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Inteligencia Artificial", "Machine Learning");
                addKeywordsToUser(ana, keywordRepository, KeywordType.LINEA_INTERES, "Desarrollo Sostenible");
                userRepository.save(ana);
            }

            // Usuario 2: Carlos Ruiz
            if (userRepository.findByEmail("carlos.ruiz@email.com").isEmpty()) {
                User carlos = User.builder()
                        .username("Carlos Ruiz")
                        .email("carlos.ruiz@email.com")
                        .imageUrl("https://plus.unsplash.com/premium_photo-1689568158814-3b8e9c1a9618?ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8MXx8JTIzcGVyc29uYXxlbnwwfHwwfHx8MA%3D%3D&fm=jpg&q=60&w=3000")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("Argentina").ciudad("Córdoba")
                        .latitud(-31.4201).longitud(-64.1888)
                        .motivaciones("Interesado en la genómica y la bioinformática.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                carlos.addEducacion(Educacion.builder().institucion("UNC").titulo("Maestría en Biología Molecular").build());
                addKeywordsToUser(carlos, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Biología Molecular");
                addKeywordsToUser(carlos, keywordRepository, KeywordType.LINEA_INTERES, "Divulgación Científica");
                userRepository.save(carlos);
            }

            // Usuario 3: Sofia Torres
            if (userRepository.findByEmail("sofia.torres@email.com").isEmpty()) {
                User sofia = User.builder()
                        .username("Sofia Torres")
                        .email("sofia.torres@email.com")
                        .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTefdAYZ6uy2rn4ODl9zSL1KwCAhiEPo9Xm-g&s")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(comunicadorRole))
                        .pais("Chile").ciudad("Santiago")
                        .latitud(-33.4489).longitud(-70.6693)
                        .motivaciones("Mi foco es la comunicación de la ciencia.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                sofia.addEducacion(Educacion.builder().institucion("PUC").titulo("Licenciatura en Comunicación Social").build());
                addKeywordsToUser(sofia, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Sociología Urbana");
                addKeywordsToUser(sofia, keywordRepository, KeywordType.LINEA_INTERES, "Divulgación Científica", "Desarrollo Sostenible");
                userRepository.save(sofia);
            }

            // Usuario 4: Martín Diaz
            if (userRepository.findByEmail("martin.diaz@email.com").isEmpty()) {
                User martin = User.builder()
                        .username("Martín Diaz")
                        .email("martin.diaz@email.com")
                        .imageUrl("https://img.freepik.com/foto-gratis/joven-hombre-barbudo-camisa-rayas_273609-5677.jpg?semt=ais_hybrid&w=740&q=80")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("Argentina").ciudad("Buenos Aires")
                        .latitud(-34.6158).longitud(-58.4333)
                        .motivaciones("Desarrollo soluciones de energía solar y eólica.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                martin.addEducacion(Educacion.builder().institucion("ITBA").titulo("Ingeniería en Energías Renovables").build());
                addKeywordsToUser(martin, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Energías Renovables");
                addKeywordsToUser(martin, keywordRepository, KeywordType.LINEA_INTERES, "Desarrollo Sostenible");
                userRepository.save(martin);
            }

            // Usuario 5: Valeria Rojas (Ciudad de México, México)
            if (userRepository.findByEmail("valeria.rojas@email.com").isEmpty()) {
                User valeria = User.builder()
                        .username("Valeria Rojas")
                        .email("valeria.rojas@email.com")
                        .imageUrl("https://img.freepik.com/foto-gratis/estilo-vida-emociones-gente-concepto-casual-confiado-agradable-sonriente-mujer-asiatica-brazos-cruzados-pecho-seguro-listo-ayudar-escuchando-companeros-trabajo-participando-conversacion_1258-59335.jpg?semt=ais_hybrid&w=740&q=80")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("México").ciudad("Ciudad de México")
                        .latitud(19.4326).longitud(-99.1332)
                        .motivaciones("Investigando el impacto de la tecnología en las comunidades urbanas de América Latina.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                valeria.addEducacion(Educacion.builder().institucion("UNAM").titulo("Doctorado en Antropología").build());
                addKeywordsToUser(valeria, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Sociología Urbana", "Antropología Digital");
                addKeywordsToUser(valeria, keywordRepository, KeywordType.LINEA_INTERES, "Educación");
                userRepository.save(valeria);
            }

            // Usuario 6: Javier Morales (Lima, Perú)
            if (userRepository.findByEmail("javier.morales@email.com").isEmpty()) {
                User javier = User.builder()
                        .username("Javier Morales")
                        .email("javier.morales@email.com")
                        .imageUrl("https://media.istockphoto.com/id/1171169099/es/foto/hombre-con-brazos-cruzados-aislados-sobre-fondo-gris.jpg?s=612x612&w=0&k=20&c=8qDLKdLMm2i8DHXY6crX6a5omVh2IxqrOxJV2QGzgFg=")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(comunicadorRole))
                        .pais("Perú").ciudad("Lima")
                        .latitud(-12.0464).longitud(-77.0428)
                        .motivaciones("Apasionado por crear contenido multimedia que conecte la ciencia con el público general.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                javier.addEducacion(Educacion.builder().institucion("UP").titulo("Maestría en Periodismo").build());
                addKeywordsToUser(javier, keywordRepository, KeywordType.LINEA_INTERES, "Divulgación Científica", "Arte y Tecnología");
                userRepository.save(javier);
            }

            // Usuario 7: Lucia Fernandez (Madrid, España)
            if (userRepository.findByEmail("lucia.fernandez@email.com").isEmpty()) {
                User lucia = User.builder()
                        .username("Lucia Fernandez")
                        .email("lucia.fernandez@email.com")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("España").ciudad("Madrid")
                        .latitud(40.4168).longitud(-3.7038)
                        .motivaciones("Explorando la intersección entre la creatividad humana y la IA generativa.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                lucia.addEducacion(Educacion.builder().institucion("UPM").titulo("Doctorado en Inteligencia Artificial").build());
                addKeywordsToUser(lucia, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Inteligencia Artificial");
                addKeywordsToUser(lucia, keywordRepository, KeywordType.LINEA_INTERES, "Arte y Tecnología");
                userRepository.save(lucia);
            }

            // Usuario 8: David Peña (Bogotá, Colombia)
            if (userRepository.findByEmail("david.pena@email.com").isEmpty()) {
                User david = User.builder()
                        .username("David Peña")
                        .email("david.pena@email.com")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("Colombia").ciudad("Bogotá")
                        .latitud(4.7110).longitud(-74.0721)
                        .motivaciones("Buscando soluciones prácticas y sostenibles para la gestión de recursos en grandes ciudades.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                david.addEducacion(Educacion.builder().institucion("Uniandes").titulo("Maestría en Ciencias Ambientales").build());
                addKeywordsToUser(david, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Energías Renovables");
                addKeywordsToUser(david, keywordRepository, KeywordType.LINEA_INTERES, "Desarrollo Sostenible");
                userRepository.save(david);
            } // Usuario 9: Pedro Pascal (Santiago, Chile) 
            if (userRepository.findByEmail("pedro.pascal@email.com").isEmpty()) {
                User pedro = User.builder()
                        .username("Pedro Pascal")
                        .email("pedro.pascal@email.com")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(comunicadorRole))
                        .pais("Chile").ciudad("Santiago")
                        .latitud(-33.4500).longitud(-70.6700)
                        .motivaciones("Conectando la ciencia con la gente a través de nuevos medios.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                pedro.addEducacion(Educacion.builder().institucion("UChile").titulo("Postgrado en Comunicación").build());

                addKeywordsToUser(pedro, keywordRepository, KeywordType.LINEA_INTERES, "Divulgación Científica", "Desarrollo Sostenible");
                userRepository.save(pedro);
            }

            // Usuario 10: Isidora Guzman (Valparaíso, Chile)
            if (userRepository.findByEmail("isidora.guzman@email.com").isEmpty()) {
                User isidora = User.builder()
                        .username("Isidora Guzman")
                        .email("isidora.guzman@email.com")
                        .imageUrl("https://st2.depositphotos.com/3822073/7201/i/450/depositphotos_72015685-stock-photo-close-up-portrait-of-a.jpg")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("Chile").ciudad("Valparaíso")
                        .latitud(-33.0472).longitud(-71.6127)
                        .motivaciones("Solo arte y tecnología.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                isidora.addEducacion(Educacion.builder().institucion("Adolfo Ibáñez").titulo("Ingeniería").build());
                addKeywordsToUser(isidora, keywordRepository, KeywordType.LINEA_INTERES, "Arte y Tecnología");
                userRepository.save(isidora);
            }

            // Usuario 11: Benjamín Rojas (Mendoza, Argentina) - Relevante pero más lejos
            if (userRepository.findByEmail("benjamin.rojas@email.com").isEmpty()) {
                User benjamin = User.builder()
                        .username("Benjamín Rojas")
                        .email("benjamin.rojas@email.com")
                        .imageUrl("https://www.shutterstock.com/image-photo/young-handsome-hicpanic-man-smiling-260nw-2527368779.jpg")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("Argentina").ciudad("Mendoza")
                        .latitud(-32.8895).longitud(-68.8458)
                        .motivaciones("Estudio el comportamiento social en entornos urbanos.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();

                benjamin.addEducacion(Educacion.builder().institucion("UNCuyo").titulo("Sociología").build());
                addKeywordsToUser(benjamin, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Sociología Urbana");
                addKeywordsToUser(benjamin, keywordRepository, KeywordType.LINEA_INTERES, "Desarrollo Sostenible", "Educación");
                userRepository.save(benjamin);
            }

            // Usuario 12: Isabella Castillo (Montevideo, Uruguay)
            if (userRepository.findByEmail("isabella.castillo@email.com").isEmpty()) {
                User isabella = User.builder()
                        .username("Isabella Castillo")
                        .email("isabella.castillo@email.com")
                        .imageUrl("https://concepto.de/wp-content/uploads/2018/08/persona-e1533759204552.jpg")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(comunicadorRole))
                        .pais("Uruguay").ciudad("Montevideo")
                        .latitud(-34.9011).longitud(-56.1645)
                        .motivaciones("Fomentando la educación para un futuro sostenible en LATAM.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();
                isabella.addEducacion(Educacion.builder().institucion("UdelaR").titulo("Licenciatura en Educación").build());
                addKeywordsToUser(isabella, keywordRepository, KeywordType.LINEA_INTERES, "Educación", "Desarrollo Sostenible");
                userRepository.save(isabella);
            }

            // Usuario 13: Mateo Navarro (Asunción, Paraguay)
            if (userRepository.findByEmail("mateo.navarro@email.com").isEmpty()) {
                User mateo = User.builder()
                        .username("Mateo Navarro")
                        .email("mateo.navarro@email.com")
                        .imageUrl("https://static.vecteezy.com/system/resources/thumbnails/009/887/693/small/male-man-african-american-black-diversity-person-afro-hair-ethnic-happy-smile-model-close-up-face-enjoyment-hashion-lifestyle-professional-human-father-boy-business-education-young-adult-teenage-photo.jpg")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("Paraguay").ciudad("Asunción")
                        .latitud(-25.2637).longitud(-57.5759)
                        .motivaciones("Investigación en almacenamiento de energía y redes inteligentes.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();
                mateo.addEducacion(Educacion.builder().institucion("UNA").titulo("Ingeniería Electromecánica").build());
                addKeywordsToUser(mateo, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Energías Renovables");
                userRepository.save(mateo);
            }

            // Usuario 14: Camila Silva (São Paulo, Brazil)
            if (userRepository.findByEmail("camila.silva@email.com").isEmpty()) {
                User camila = User.builder()
                        .username("Camila Silva")
                        .email("camila.silva@email.com")
                        .imageUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQKZtqfXYLb6STY0Ljvd4BavB1XgJoIGoi73A&s")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("Brasil").ciudad("São Paulo")
                        .latitud(-23.5505).longitud(-46.6333)
                        .motivaciones("Aplicando machine learning para la creación de arte generativo.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();
                camila.addEducacion(Educacion.builder().institucion("USP").titulo("Maestría en Ciencias de la Computación").build());
                addKeywordsToUser(camila, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Machine Learning");
                addKeywordsToUser(camila, keywordRepository, KeywordType.LINEA_INTERES, "Arte y Tecnología");
                userRepository.save(camila);
            }

            // Usuario 15: Lucas Ferreira (Rio de Janeiro, Brazil)
            if (userRepository.findByEmail("lucas.ferreira@email.com").isEmpty()) {
                User lucas = User.builder()
                        .username("Lucas Ferreira")
                        .email("lucas.ferreira@email.com")
                        .imageUrl("https://media.istockphoto.com/id/1386479313/es/foto/feliz-mujer-de-negocios-afroamericana-millennial-posando-aislada-en-blanco.jpg?s=612x612&w=0&k=20&c=JP0NBxlxG2-bdpTRPlTXBbX13zkNj0mR5g1KoOdbtO4=")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(comunicadorRole))
                        .pais("Brasil").ciudad("Rio de Janeiro")
                        .latitud(-22.9068).longitud(-43.1729)
                        .motivaciones("Documentando la vida en las favelas y su transformación social.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();
                lucas.addEducacion(Educacion.builder().institucion("UFRJ").titulo("Periodismo").build());
                addKeywordsToUser(lucas, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Sociología Urbana");
                addKeywordsToUser(lucas, keywordRepository, KeywordType.LINEA_INTERES, "Divulgación Científica");
                userRepository.save(lucas);
            }

            // Usuario 16: Gabriela Moreno (Quito, Ecuador)
            if (userRepository.findByEmail("gabriela.moreno@email.com").isEmpty()) {
                User gabriela = User.builder()
                        .username("Gabriela Moreno")
                        .email("gabriela.moreno@email.com")
                        .imageUrl("https://thumbs.dreamstime.com/b/cara-real-de-la-persona-34700593.jpg")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("Ecuador").ciudad("Quito")
                        .latitud(-0.1807).longitud(-78.4678)
                        .motivaciones("Estudio de ecosistemas de alta montaña y su conservación.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();
                gabriela.addEducacion(Educacion.builder().institucion("EPN").titulo("Doctorado en Biología").build());
                addKeywordsToUser(gabriela, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Biología Molecular");
                addKeywordsToUser(gabriela, keywordRepository, KeywordType.LINEA_INTERES, "Desarrollo Sostenible");
                userRepository.save(gabriela);
            }

            // Usuario 17: Sebastián Cruz (Panama City, Panama)
            if (userRepository.findByEmail("sebastian.cruz@email.com").isEmpty()) {
                User sebastian = User.builder()
                        .username("Sebastián Cruz")
                        .email("sebastian.cruz@email.com")
                        .imageUrl("https://st2.depositphotos.com/1662991/8837/i/450/depositphotos_88370500-stock-photo-mechanic-wearing-overalls.jpg")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(comunicadorRole))
                        .pais("Panamá").ciudad("Ciudad de Panamá")
                        .latitud(8.9824).longitud(-79.5199)
                        .motivaciones("Logística y comercio para un desarrollo sostenible en la región.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();
                sebastian.addEducacion(Educacion.builder().institucion("UTP").titulo("Logística").build());
                addKeywordsToUser(sebastian, keywordRepository, KeywordType.LINEA_INTERES, "Desarrollo Sostenible");
                userRepository.save(sebastian);
            }

            // Usuario 18: Elena Flores (Vancouver, Canada) - Muy relevante para Ana, pero muy lejana
            if (userRepository.findByEmail("elena.flores@email.com").isEmpty()) {
                User elena = User.builder()
                        .username("Elena Flores")
                        .email("elena.flores@email.com")
                        .imageUrl("https://www.shutterstock.com/image-photo/young-brunette-woman-standing-over-260nw-2248467101.jpg")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("Canadá").ciudad("Vancouver")
                        .latitud(49.2827).longitud(-123.1207)
                        .motivaciones("Deep Learning aplicado a la interpretación de datos complejos. Busco partners en LATAM.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();
                elena.addEducacion(Educacion.builder().institucion("UBC").titulo("Postdoctorado en IA").build());
                addKeywordsToUser(elena, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Inteligencia Artificial", "Machine Learning");
                addKeywordsToUser(elena, keywordRepository, KeywordType.LINEA_INTERES, "Desarrollo Sostenible");
                userRepository.save(elena);
            }

            // Usuario 19: Daniel Vega (Barcelona, España)
            if (userRepository.findByEmail("daniel.vega@email.com").isEmpty()) {
                User daniel = User.builder()
                        .username("Daniel Vega")
                        .email("daniel.vega@email.com")
                        .imageUrl("https://st.depositphotos.com/1011382/2845/i/950/depositphotos_28451603-stock-photo-real-normal-person-portrait.jpg")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("España").ciudad("Barcelona")
                        .latitud(41.3851).longitud(2.1734)
                        .motivaciones("El urbanismo táctico y la tecnología como herramientas de cambio social.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();
                daniel.addEducacion(Educacion.builder().institucion("UPC").titulo("Arquitectura").build());
                addKeywordsToUser(daniel, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Sociología Urbana");
                addKeywordsToUser(daniel, keywordRepository, KeywordType.LINEA_INTERES, "Arte y Tecnología");
                userRepository.save(daniel);
            }

            // Usuario 20: Ricardo Paredes (La Paz, Bolivia)
            if (userRepository.findByEmail("ricardo.paredes@email.com").isEmpty()) {
                User ricardo = User.builder()
                        .username("Ricardo Paredes")
                        .email("ricardo.paredes@email.com")
                        .imageUrl("https://plus.unsplash.com/premium_photo-1678197937465-bdbc4ed95815?ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8NXx8cGVyc29uYXxlbnwwfHwwfHx8MA%3D%3D&fm=jpg&q=60&w=3000")
                        .password(encoder.encode("TestPassword123!"))
                        .roles(Set.of(academicoRole))
                        .pais("Bolivia").ciudad("La Paz")
                        .latitud(-16.4897).longitud(-68.1193)
                        .motivaciones("Estudio de las identidades digitales en comunidades indígenas.")
                        .accountNoExpired(true).accountNoLocked(true).credentialNoExpired(true).isEnabled(true)
                        .build();
                ricardo.addEducacion(Educacion.builder().institucion("UMSA").titulo("Antropología").build());
                addKeywordsToUser(ricardo, keywordRepository, KeywordType.CAMPO_INVESTIGACION, "Antropología Digital");
                addKeywordsToUser(ricardo, keywordRepository, KeywordType.LINEA_INTERES, "Educación");
                userRepository.save(ricardo);
            }

            // --- 4. Eventos de ejemplo ---
            if (eventRepository.count() == 0) {
                var anaOpt = userRepository.findByEmail("ana.gomez@email.com");
                var carlosOpt = userRepository.findByEmail("carlos.ruiz@email.com");
                var sofiaOpt = userRepository.findByEmail("sofia.torres@email.com");

                Event ev1 = Event.builder()
                        .title("Charla: IA y Sociedad")
                        .type(EventType.CHARLA)
                        .date(java.time.LocalDateTime.now().plusDays(10))
                        .ubication("Auditorio Central")
                        .description("Una charla sobre los impactos sociales y éticos de la Inteligencia Artificial.")
                        .imageUrl("https://images.unsplash.com/photo-1529333166437-7750a6dd5a70")
                        .user(anaOpt.orElse(null))
                        .isEnabled(true)
                        .build();
                eventRepository.save(ev1);

                Event ev2 = Event.builder()
                        .title("Congreso de Biología Molecular")
                        .type(EventType.CONGRESO)
                        .date(java.time.LocalDateTime.now().plusDays(30))
                        .ubication("Centro de Convenciones")
                        .description("Congreso internacional sobre avances en biología molecular.")
                        .imageUrl("https://images.unsplash.com/photo-1522199710521-72d69614c702?auto=format&fit=crop&w=800&q=80")
                        .user(carlosOpt.orElse(null))
                        .isEnabled(true)
                        .build();
                eventRepository.save(ev2);

                Event ev3 = Event.builder()
                        .title("Festival de Divulgación Científica")
                        .type(EventType.CONCURSO)
                        .date(java.time.LocalDateTime.now().plusDays(20))
                        .ubication("Plaza Central")
                        .description("Actividades y stands para acercar la ciencia a la comunidad.")
                        .imageUrl("https://images.unsplash.com/photo-1503387762-592deb58ef4e")
                        .user(sofiaOpt.orElse(null))
                        .isEnabled(true)
                        .build();
                eventRepository.save(ev3);
            }

                        // Asegurarse de que todos los eventos tengan una imageUrl (usar placeholder por tipo si falta)
                        var allEvents = eventRepository.findAll();
                        boolean changed = false;
                        for (Event e : allEvents) {
                                // Force-replace the image for the seeded 'Congreso de Biología Molecular' (evento 2)
                                if ("Congreso de Biología Molecular".equals(e.getTitle())) {
                                        String newImg = "https://images.unsplash.com/photo-1522199710521-72d69614c702?auto=format&fit=crop&w=800&q=80";
                                        if (!newImg.equals(e.getImageUrl())) {
                                                e.setImageUrl(newImg);
                                                changed = true;
                                        }
                                        continue; // we updated or ensured this one
                                }

                                if (e.getImageUrl() == null || e.getImageUrl().isBlank()) {
                                        String defaultImg;
                                        if (e.getType() == EventType.CHARLA) {
                                                defaultImg = "https://images.unsplash.com/photo-1509062522246-3755977927d7";
                                        } else if (e.getType() == EventType.CONGRESO) {
                                                defaultImg = "https://images.unsplash.com/photo-1517430816045-df4b7de11d1d";
                                        } else if (e.getType() == EventType.CONCURSO) {
                                                defaultImg = "https://images.unsplash.com/photo-1526312426976-3d4e0aa5b0c9";
                                        } else {
                                                defaultImg = "https://images.unsplash.com/photo-1496307042754-b4aa456c4a2d";
                                        }
                                        e.setImageUrl(defaultImg);
                                        changed = true;
                                }
                        }
                        if (changed) {
                                eventRepository.saveAll(allEvents);
                        }
        };
    }

    private void addKeywordsToUser(User user, KeywordRepository repo, KeywordType type, String... keywordNames) {
        Arrays.stream(keywordNames)
                .forEach(name -> repo.findByName(name).ifPresent(keyword -> {
                    UserKeyword userKeyword = UserKeyword.builder()
                            .keyword(keyword)
                            .type(type)
                            .build();
                    user.addKeyword(userKeyword);
                }));
    }
}