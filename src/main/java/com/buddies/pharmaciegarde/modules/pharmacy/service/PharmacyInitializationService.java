package com.buddies.pharmaciegarde.modules.pharmacy.service;

import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy;
import com.buddies.pharmaciegarde.modules.pharmacy.model.Pharmacy.PharmacyStatus;
import com.buddies.pharmaciegarde.modules.pharmacy.repository.PharmacyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service d'initialisation pour charger les pharmacies depuis un JSON
 * Respecte la convention de nommage PHCIE (et non PHARMACIE)
 * Détecte les doublons par nom + commune + ville + district
 */
@Service
@Order(2) // Exécuter après l'initialisation des zones
public class PharmacyInitializationService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PharmacyInitializationService.class);

    private final PharmacyRepository pharmacyRepository;
    private final boolean seedEnabled;

    public PharmacyInitializationService(PharmacyRepository pharmacyRepository,
            @Value("${app.seed.pharmacies.enabled:false}") boolean seedEnabled) {
        this.pharmacyRepository = pharmacyRepository;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            logger.info("Initialisation des pharmacies désactivée (app.seed.pharmacies.enabled=false)");
            return;
        }

        logger.info("Début de l'initialisation des pharmacies...");

        List<PharmacyData> pharmaciesData = getPharmaciesData();
        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (PharmacyData data : pharmaciesData) {
            try {
                boolean wasCreated = processPharmacy(data);
                if (wasCreated) {
                    created++;
                } else {
                    updated++;
                }
            } catch (Exception e) {
                logger.error("Erreur lors du traitement de la pharmacie {}: {}", data.name, e.getMessage(), e);
                skipped++;
            }
        }

        logger.info("Initialisation des pharmacies terminée: {} créées, {} mises à jour, {} ignorées", created, updated, skipped);
    }

    /**
     * Traite une pharmacie dans sa propre transaction
     * Cela permet qu'une erreur sur une pharmacie n'affecte pas les autres
     * @return true si la pharmacie a été créée, false si elle a été mise à jour
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processPharmacy(PharmacyData data) {
        // Normaliser le nom (remplacer "Pharmacie" par "PHCIE")
        String normalizedName = normalizePharmacyName(data.name);

        // Chercher une pharmacie existante par nom + commune + ville + district
        Optional<Pharmacy> existingPharmacy = pharmacyRepository
                .findByNameAndCommuneAndCityAndDistrict(
                        normalizedName,
                        data.commune != null ? data.commune.trim() : "",
                        data.city != null ? data.city.trim() : "",
                        data.district != null ? data.district.trim() : null
                );

        if (existingPharmacy.isPresent()) {
            // Mettre à jour la pharmacie existante
            Pharmacy pharmacy = existingPharmacy.get();
            updatePharmacy(pharmacy, data, normalizedName);
            pharmacyRepository.save(pharmacy);
            logger.debug("Pharmacie mise à jour: {} ({}, {}, {})", normalizedName, data.city, data.commune, data.district);
            return false; // Mise à jour
        } else {
            // Créer une nouvelle pharmacie
            Pharmacy pharmacy = createPharmacy(data, normalizedName);
            pharmacyRepository.save(pharmacy);
            logger.debug("Pharmacie créée: {} ({}, {}, {})", normalizedName, data.city, data.commune, data.district);
            return true; // Création
        }
    }

    /**
     * Normalise le nom de la pharmacie: remplace "Pharmacie" ou "PHIE" par "PHCIE"
     */
    private String normalizePharmacyName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }
        String normalized = name.trim();

        // Remplacer "PHARMACIE" par "PHCIE" (insensible à la casse)
        normalized = normalized.replaceAll("(?i)\\bPHARMACIE\\b", "PHCIE");

        // Remplacer "PHIE" par "PHCIE" (insensible à la casse)
        normalized = normalized.replaceAll("(?i)\\bPHIE\\b", "PHCIE");

        // Si le nom ne commence pas par PHCIE, l'ajouter
        if (!normalized.toUpperCase().startsWith("PHCIE")) {
            normalized = "PHCIE " + normalized;
        }

        return normalized;
    }

    /**
     * Crée une nouvelle pharmacie
     */
    private Pharmacy createPharmacy(PharmacyData data, String normalizedName) {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setName(normalizedName);
        pharmacy.setPhone1(data.phone1 != null ? data.phone1.trim() : "00000000");
        pharmacy.setPhone2(data.phone2 != null && !data.phone2.trim().isEmpty() ? data.phone2.trim() : null);
        pharmacy.setEmail(data.email != null && !data.email.trim().isEmpty() ? data.email.trim() : null);
        pharmacy.setHoraires(data.horaires != null ? data.horaires.trim() : null);
        pharmacy.setCity(data.city != null ? data.city.trim() : "");
        pharmacy.setCommune(data.commune != null ? data.commune.trim() : "");
        pharmacy.setDistrict(data.district != null && !data.district.trim().isEmpty() ? data.district.trim() : null);
        pharmacy.setDescription(data.description != null ? data.description.trim() : null);
        pharmacy.setPharmacistName(data.pharmacistName != null ? data.pharmacistName.trim() : null);
        pharmacy.setPhoto(data.photo != null && !data.photo.trim().isEmpty() && !"string".equalsIgnoreCase(data.photo.trim()) ? data.photo.trim() : null);
        pharmacy.setStatut(PharmacyStatus.ACTIF);

        // Construire le champ location au format "lat,lon"
        String location = buildLocationString(data.latitude, data.longitude);
        pharmacy.setLocation(location);

        return pharmacy;
    }

    /**
     * Met à jour une pharmacie existante
     */
    private void updatePharmacy(Pharmacy pharmacy, PharmacyData data, String normalizedName) {
        // Ne pas changer le nom si déjà normalisé
        if (!pharmacy.getName().equals(normalizedName)) {
            pharmacy.setName(normalizedName);
        }

        // Mettre à jour les autres champs
        if (data.phone1 != null && !data.phone1.trim().isEmpty()) {
            pharmacy.setPhone1(data.phone1.trim());
        }
        if (data.phone2 != null && !data.phone2.trim().isEmpty()) {
            pharmacy.setPhone2(data.phone2.trim());
        } else if (data.phone2 == null || data.phone2.trim().isEmpty()) {
            pharmacy.setPhone2(null);
        }
        if (data.email != null && !data.email.trim().isEmpty()) {
            pharmacy.setEmail(data.email.trim());
        } else if (data.email == null || data.email.trim().isEmpty()) {
            pharmacy.setEmail(null);
        }
        if (data.horaires != null) {
            pharmacy.setHoraires(data.horaires.trim());
        }
        if (data.city != null && !data.city.trim().isEmpty()) {
            pharmacy.setCity(data.city.trim());
        }
        if (data.commune != null && !data.commune.trim().isEmpty()) {
            pharmacy.setCommune(data.commune.trim());
        }
        if (data.district != null && !data.district.trim().isEmpty()) {
            pharmacy.setDistrict(data.district.trim());
        } else if (data.district == null || data.district.trim().isEmpty()) {
            pharmacy.setDistrict(null);
        }
        if (data.description != null) {
            pharmacy.setDescription(data.description.trim());
        }
        if (data.pharmacistName != null) {
            pharmacy.setPharmacistName(data.pharmacistName.trim());
        }
        if (data.photo != null && !data.photo.trim().isEmpty() && !"string".equalsIgnoreCase(data.photo.trim())) {
            pharmacy.setPhoto(data.photo.trim());
        }

        // Mettre à jour la localisation si disponible
        if (data.latitude != null && data.longitude != null) {
            String location = buildLocationString(data.latitude, data.longitude);
            pharmacy.setLocation(location);
        }
    }

    /**
     * Construit la chaîne de localisation au format "lat,lon"
     */
    private String buildLocationString(Double latitude, Double longitude) {
        if (latitude != null && longitude != null) {
            // Nettoyer les espaces dans la longitude si présents
            String lonStr = longitude.toString().trim().replaceAll("\\s+", "");
            return latitude + "," + lonStr;
        }
        return "0,0"; // Valeur par défaut
    }

    /**
     * Helper method pour créer une PharmacyData depuis le format JSON fourni
     * @param etablissement Nom de la pharmacie (Etablissement) - dans le JSON c'est le nom de la pharmacie
     * @param nomPharmacien Nom du pharmacien (Nom) - dans le JSON c'est le nom du pharmacien
     * @param telEmail Téléphone ou email (TEL/EMAIL)
     * @param adresse Adresse (ADRESSE)
     * @param villeRaw Ville au format "VILLE" ou "VILLE/COMMUNE" ou "VILLE/COMMUNE/DISTRICT"
     */
    private PharmacyData createPharmacyDataFromJsonFormat(String etablissement, String nomPharmacien,
                                                          String telEmail, String adresse, String villeRaw) {
        // Le nom de la pharmacie est dans "Etablissement"
        String name = etablissement != null ? etablissement.trim() : "";

        // Parser VILLE pour extraire ville, commune, district
        String[] villeCommuneDistrict = parseVilleCommuneDistrict(villeRaw, adresse);
        String city = villeCommuneDistrict[0];
        String commune = villeCommuneDistrict[1];
        String district = villeCommuneDistrict.length > 2 && villeCommuneDistrict[2] != null && !villeCommuneDistrict[2].isEmpty() ? villeCommuneDistrict[2] : null;

        // Parser TEL/EMAIL pour extraire téléphone et email
        String[] telEmailArray = parseTelEmail(telEmail);
        String phone1 = telEmailArray[0];
        String email = telEmailArray[1];

        return new PharmacyData(name, phone1, null, email, null, city, commune, district,
                               adresse, nomPharmacien, null, null, null);
    }

    /**
     * Parse le champ VILLE pour extraire ville, commune, district
     * Formats supportés: "ABIDJAN", "ABIDJAN/PLATEAU", "ABIDJAN/PLATEAU/DISTRICT", "RCI", "San Pédro"
     */
    private String[] parseVilleCommuneDistrict(String villeRaw, String adresse) {
        if (villeRaw == null || villeRaw.trim().isEmpty() || villeRaw.trim().equals("RCI")) {
            // Si VILLE = "RCI" ou vide, essayer de déduire depuis l'adresse
            if (adresse != null && !adresse.trim().isEmpty()) {
                String adr = adresse.trim().toUpperCase();
                if (adr.contains("SAN-PEDRO") || adr.contains("SAN PEDRO")) {
                    return new String[]{"SAN-PÉDRO", "SAN-PÉDRO", null};
                } else if (adr.contains("ABIDJAN")) {
                    return new String[]{"ABIDJAN", "ABIDJAN", null};
                }
            }
            return new String[]{"ABIDJAN", "ABIDJAN", null};
        }

        String[] parts = villeRaw.trim().split("/");
        String city = parts[0].trim();
        String commune = parts.length > 1 ? parts[1].trim() : city; // Utiliser la ville comme commune par défaut
        String district = parts.length > 2 ? parts[2].trim() : null;

        // Normaliser les villes connues
        if (city.equalsIgnoreCase("San Pédro") || city.equalsIgnoreCase("SAN-PEDRO")) {
            city = "SAN-PÉDRO";
            commune = "SAN-PÉDRO";
        } else if (city.equalsIgnoreCase("RIVIERA")) {
            city = "ABIDJAN";
            commune = "COCODY";
        } else if (city.equalsIgnoreCase("COCODY")) {
            city = "ABIDJAN";
        } else if (city.equalsIgnoreCase("YOPOUGON")) {
            city = "ABIDJAN";
        } else if (city.equalsIgnoreCase("Abidjan")) {
            city = "ABIDJAN";
        }

        return new String[]{city, commune, district};
    }

    /**
     * Parse le champ TEL/EMAIL pour séparer téléphone et email
     */
    private String[] parseTelEmail(String telEmail) {
        String phone = "00000000";
        String email = null;

        if (telEmail != null && !telEmail.trim().isEmpty()) {
            String value = telEmail.trim();

            // Chercher un email (contient @)
            if (value.contains("@")) {
                // Extraire l'email (peut être entre < > ou directement)
                int atIndex = value.indexOf('@');
                int startEmail = 0;
                int endEmail = value.length();

                // Chercher < avant @
                int ltIndex = value.lastIndexOf('<', atIndex);
                if (ltIndex != -1) {
                    startEmail = ltIndex + 1;
                    int gtIndex = value.indexOf('>', atIndex);
                    if (gtIndex != -1) {
                        endEmail = gtIndex;
                    }
                } else {
                    // Pas de < >, chercher le début (espace, début de chaîne)
                    startEmail = value.lastIndexOf(' ', atIndex) + 1;
                    if (startEmail == 0) {
                        startEmail = 0;
                    }
                    // Chercher la fin (espace, /, fin de chaîne)
                    for (int i = atIndex + 1; i < value.length(); i++) {
                        char c = value.charAt(i);
                        if (c == ' ' || c == '/' || c == ',') {
                            endEmail = i;
                            break;
                        }
                    }
                }
                email = value.substring(startEmail, endEmail).trim();
            }

            // Chercher un téléphone seulement s'il n'y a pas d'email ou si la valeur contient aussi des chiffres
            String cleanValue = value.replaceAll("[^+0-9\\s-]", " ").trim();
            if (!cleanValue.isEmpty() && (cleanValue.startsWith("+") || cleanValue.matches(".*\\d{8,}.*"))) {
                String[] parts = cleanValue.split("[,\\s/]+");
                for (String part : parts) {
                    part = part.trim();
                    if (!part.isEmpty() && (part.startsWith("+") || part.matches(".*\\d{8,}.*"))) {
                        phone = part;
                        break;
                    }
                }
            }
        }

        return new String[]{phone, email};
    }

    /**
     * Retourne la liste des pharmacies à initialiser
     */
    private List<PharmacyData> getPharmaciesData() {
        List<PharmacyData> pharmacies = new ArrayList<>();

        pharmacies.add(new PharmacyData("Pharmacie Riviera 3", "+225 27 22 47 56 16", null, "phcieriviera3@gmail.com",
                "Lundi-Samedi: 08h-21h, Dimanche: Selon garde", "Abidjan", "Cocody", "Riviera 3",
                "Située à la Riviera 3, non loin du carrefour 9 kilos.", "Dr. Kanga Yao Félix", "string",
                5.3524, -3.9511));

        pharmacies.add(new PharmacyData("Pharmacie Moderne Mazuet", "+225 27 20 32 96 64", null, "phciemazuet@aviso.ci",
                "Lundi-Vendredi: 07h30-19h30, Samedi: 08h-13h", "Abidjan", "Plateau", "Centre-Ville",
                "Rue du commerce, 4 Av. du Général de Gaulle.", "Dr. Kouassi Parfait", "string",
                5.3218, -4.0195));

        pharmacies.add(new PharmacyData("Pharmacie des Finances", "+225 27 20 30 39 50", null, "emmaangoua@yahoo.fr",
                "Lundi-Samedi: 08h-20h", "Abidjan", "Plateau", "Quartier des Affaires",
                "À proximité du Trésor Public.", "Dr. Angoua Tanoé Emma Clarisse", "string",
                5.3289, -4.0172));

        pharmacies.add(new PharmacyData("Pharmacie de la Cité", "+225 27 24 39 03 33", null, null,
                "Lundi-Dimanche: 24h/24 (Gardes régulières)", "Abidjan", "Abobo", "Sogefiha",
                "Abobo gare, quartier Sogefiha.", "Dr. Egnankou Juliette", "string",
                5.4181, -4.0131));

        pharmacies.add(new PharmacyData("Pharmacie Iduriel", "+225 05 55 74 06 18", "+225 07 49 37 95 73", null,
                "Lundi-Samedi: 08h-21h", "Bingerville", "Bingerville", "Koffikro",
                "Carrefour Bassamoise, non loin de Côte d'Ivoire Énergie.", "Dr. Soma Kyna Auguste Epse Angama", "string",
                5.3562, -3.8864));

        pharmacies.add(new PharmacyData("Pharmacie Havila de Sifié", "+225 07 07 89 21 21", null, null,
                "Lundi-Samedi: 08h-20h", "Sifié", "Séguéla", "Worodougou",
                "Pharmacie principale de la ville de Sifié.", "Dr. Goore Bi Djangonai Christian", "string",
                8.0667, -6.6500));

        pharmacies.add(new PharmacyData("Pharmacie Odelia", "+225 27 33 79 11 00", null, null,
                "7j/7 selon planning", "Man", "Man", "Tonkpi",
                "Située dans la ville de Man, région du Tonkpi.", "Dr. Koidio Clément Koissi", "string",
                7.4125, -7.5538));

        pharmacies.add(new PharmacyData("Pharmacie Marcory PTT Cession", "+225 27 21 26 25 36", null, "nouvellePHCIEptt@gmail.com",
                "Lundi-Samedi: 08h-21h", "Abidjan", "Marcory", "Marcory Résidentiel",
                "Zone PTT, proche de la poste de Marcory.", "Dr. Kamel Sayegh", "string",
                5.3031, -3.9934));

        pharmacies.add(new PharmacyData("Pharmacie Atlantique", "+225 27 21 27 10 10", null, "kanigui@yahoo.com",
                "Lundi-Samedi: 08h-20h30", "Abidjan", "Port-Bouët", "Centre",
                "À proximité du marché de Port-Bouët.", "Dr. Ouattara Kanigui", "string",
                5.2536, -3.9452));

        pharmacies.add(new PharmacyData("Pharmacie du Soleil", "+225 27 21 36 05 98", null, null,
                "Lundi-Samedi: 08h-21h", "Abidjan", "Koumassi", "Koumassi Centre",
                "Angle Avenue 9, Boulevard du Gabon, face Ecobank.", "Dr. Krou Kouame", "string",
                5.3039, -3.9491));

        pharmacies.add(new PharmacyData("Pharmacie Saint Christophe", "+225 27 22 42 23 76", null, null,
                "Lundi-Samedi: 08h-21h", "Abidjan", "Cocody", "Angré",
                "Angré Boulevard Latrille, Carrefour Petro Ivoire.", "Dr. N'Dri Kouamé", "string",
                5.3985, -3.9741));

        pharmacies.add(new PharmacyData("Pharmacie Bondoukou", "+225 07 58 68 23 51", null, null,
                "7j/7 selon garde", "Bondoukou", "Bondoukou", "Gontougo",
                "Pharmacie historique de la ville de Bondoukou.", "Dr. Philippe Kacou", "string",
                8.0333, -2.8000));

        pharmacies.add(new PharmacyData("Pharmacie Le Réel", "+225 27 21 30 30 47", null, null,
                "Lundi-Samedi: 08h-20h", "San-Pédro", "San-Pédro", "Centre",
                "Proche du port de San-Pédro.", "Dr. Erojbinou Evelyne", "string",
                4.7485, -6.6363));

        pharmacies.add(new PharmacyData("Pharmacie Les Arums", "+225 07 07 53 49 88", null, null,
                "Lundi-Samedi: 08h-21h", "Abidjan", "Cocody", "2 Plateaux",
                "8ème Tranche, Carrefour BCEAO, voie CNPS.", "Dr. Diaby Lanciné", "string",
                5.3854, -3.9872));

        pharmacies.add(new PharmacyData("Pharmacie Bécedi-Brignan", "+225 01 02 34 69 72", null, null,
                "Lundi-Samedi: 08h-19h", "Bécedi", "Adzopé", "La Mé",
                "Officine rurale desservant la zone de Bécedi.", "Dr. Yapi Kossan", "string",
                6.1014, -3.8619));

        pharmacies.add(new PharmacyData("Pharmacie Makissi", "+225 27 20 37 70 39", null, null,
                "Lundi-Samedi: 08h-20h", "Abidjan", "Adjamé", "220 Logements",
                "Derrière le Cinéma Liberté.", "Dr. Coulibaly Makissi", "string",
                5.3521, -4.0185));

        pharmacies.add(new PharmacyData("Pharmacie Sainte Odile", "+225 27 24 39 29 97", null, null,
                "Lundi-Samedi: 08h-21h", "Abidjan", "Abobo", "Dokui",
                "Secteur Dokui, proche de la station.", "Dr. Soro Odile", "string",
                5.3912, -4.0021));

        pharmacies.add(new PharmacyData("Pharmacie Redemption", "+225 25 22 01 61 96", "+225 07 89 43 63 70", null,
                "Lundi-Samedi: 08h-22h", "Abidjan", "Cocody", "Angré Château",
                "Située au niveau du Château d'eau d'Angré.", "Dr. Kouassi Amoin", "string",
                5.4124, -3.9654));

        pharmacies.add(new PharmacyData("Pharmacie Belle Cité", "+225 27 24 49 12 63", null, null,
                "Lundi-Samedi: 08h-21h", "Abidjan", "Abobo", "Belle Cité",
                "Proche du carrefour Akeikoi.", "Dr. Koné Yarapha", "string",
                5.4321, -4.0256));

        pharmacies.add(new PharmacyData("Pharmacie Hosanna", "+225 27 22 55 25 59", "+225 01 01 17 29 31", null,
                "Lundi-Samedi: 08h-22h", "Abidjan", "Cocody", "Riviera Triangle",
                "Près de la clinique Saint Viateur.", "Dr. Koffi N'Guessan", "string",
                5.3621, -3.9385));

        // Ajouter toutes les pharmacies depuis la liste JSON fournie
        // Format: Etablissement = nom pharmacie, Nom = nom pharmacien, VILLE = ville/commune, TEL/EMAIL = tel ou email

        // San Pédro
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVINE PROVIDENCE", "ABADJINAN K.AUGUSTIN", "phagma@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU GRD MARCHE SAN PEDRO", "OCHO YACINTH", "pharmacienero@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NERO (SPECIAL CNPS)", "THIAM DEIMBA - (SPECIAL C", "phcielesbeatitudessarl@hotmail.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES BEATITUDES SARL SAN-", "N'GOM ARONA", "gbohoussou@hotmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA GARE SARL", "BOHOUSSOU N'DRI GERMAIN", "zbtviessia@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE VIESSIA", "ZAMBLE BI TAH", "georgeskoffi09@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SARA-ANNA", "KOFFI GBOKRY GEORGES RUFI", "dr.yao@lapharmaciedubienetre.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU BIEN-ETRE SARL", "YAO CHARLES ALEXIS", "phiecitesp@hotmail.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE DE LA CITE (RACHAT", "MABO SERGE THIERRY", "kouadjok@live.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVINE PROVIDENCE DOGBO", "KOUADJO KAN ETIENNE", "fofanaamadou@gmail,com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUR SAN-PEDRO", "AMADOU FOFANA", "seysoubr@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BLIGNAN-YABAYO", "SEY N'DRAMAN", "kouadiondak@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BARDOT 2000", "KOUADIO N'DA", "elstef62@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DENISE", "DENISE GEORGES", "phmadou@aviso.ci", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MADOU-SARL", "DOBRE BADOBRE EPSE EPONON", "seredjo@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE CLAUDINE", "EPONONH SERGE", "pmamenet@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'ARCHANGE-SAN-PEDRO", "DOFFO CHRISTEL", "", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE DAGADJI", "AKA HERVE", "kadodjoma@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DO SARL", "COULIBALY KADODJOMA", "ngoranetchienmonique@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("SAINTE RITA SARL-U", "N'GORAN ETCHIEN MONIQUE", "marcelleattoh@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRANDE PHCIE DE SEWEKE", "ATTOH M.DELPHINE AFFAMAN", "pharmaciedespalmes@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES PALMES", "KASSI RICHARD RICHMOND", "abrahamaguido@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CHATEAU SARLU SOUBRE", "AGUIDO APPINDRE ABRAHAM", "tresornguessan@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PROVIDENCE SOUBRE", "N'GUESSAN KOUAKOU TRESOR", "phroidavid@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ROI DAVID", "ATRI KOUASSI DANIEL ANCEL", "", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE GBAGBAM", "KOUAME KOFFI FLAVIEN", "docteurdje@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE JEHOVAH-RAPHA", "DJE AHOUE RICHARD THOMSON", "", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE SAINTE DELPHINE", "DOBE REGINE EPSE AKA", "bastideweb@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE SAINTE BERNADETTE", "PITTE BASTIDE", "niangarastephane@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT ANTOINE SARL G.ZA", "NIANGARA NOGBOU BENJAMIN", "sokoyv@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRANDE PHCIE SAINTE RITA", "SOKO YVES NIABITH", "ngorankouassi814@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TERRE-ROUGE", "KOUASSI N'GORAN LAURENT", "phciefrancophonie@yahoo.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE QUARTIER FRANCOPHONIE", "KOUAME PHILIPPE OBIN", "phcielesanges@yahoo.f", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES ANGES GABIADJI SARL", "RECOLTE YAO AMANY ANNICK", "ngoranmathieu2013@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE GRIHIRI", "ASSALE CLEMENT", "hybesson@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LYADE", "OULAI HYBESSON CLARISSE", "maurice_seri@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LA GRACE DE SOTREF", "SERI PLI MAURICE", "dadi.franck@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NABENOU(RACHAT)", "DADI ZIRIGNON FRANCK", "phciemeagui@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE MEAGUI", "VARLET MENSAH", "louiseassagou@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE CLOTILDE", "ASSAGOU MICHELE", "djamacesar@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ADJAMENE", "DJAMA KOMELA GUY CESAR", "", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT BERNARD DE BAKAYO", "EZOUA ADRIEN OMER TANOE", "phciesaintemarie.sp@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE MARIE", "KOMENAN AMANI", "kraarthur2009@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DAKPADOU", "KRA ATTOUNGBRE", "baz6c@yahoo.fr / phciequartierbayard@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE QUARTIER BAYARD", "CISSE BAZOUMANA IBRAHIMA", "yahveschamma@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE YAHVE SCHAMMA", "KOUAKOU N'GORAN N. EPSE YAO", "mike_dapre@yahoo.fr / dhmdapre2477gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA MOSQUEE DE SOUBRE", "DAPRE DJOUKOUA HUGUES MICHEL", "", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU SUD OUEST (CESSION)", "SERY ZOKO AMBROISE", "", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ESPERANCE GD BEREBY", "KOFFI PIERRE", "", "San Pédro", "RCI"));

        // Abidjan
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUVELLE ALLIANCE", "KOUADIO N'GUESSAN PASCAL", "pascakouadio@yahoo.fr", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA TENE SARL-U", "YAPO GONTRAN OCTAVE", "octayap@yahoo.fr", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE MODENE VAVOUA SARL", "N'GORAN KONAN FIRMIN", "firmin_ngoran@yahoo.fr /modernevavoua@yahoo.fr", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERNE MAN SARL (TIA)", "TIA ANDRE", "ph.mod.man@gmail.com", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE EMMANUELLA", "OMBOILE JOSMINE MARIE EST", "pharmmella2017@gmail.com", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LYCEE CLUB MAN", "ABBE ELEONORE GISELE EPS AMANI", "amanielgi@gmail.com", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE ROUTE D'ISSIA", "YAVO BOKA HUBERT", "hubertyavo@gmail.com", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ADONAÏ D'ORLY", "AMONDJI EPSE YAVO C.EMMA", "yavoamondji@yahoo.fr", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AHOUKOUA", "TRA BI VANIE ALAIN", "bivanie@gmail.com", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GARE SINFRA", "EBAGNITCHIE KOREKI MICHELLE", "cecilekoreki@gmail.com", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FLOREL", "KRA YAO", "pharmacie florel <pharmacieflorel@gmail.com>", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR BOUAFLE", "KOFFI CHARLE", "carrefourbouafle pharmacie <phciecarrefourbouafle@yahoo.com>", "", "Abidjan"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EAU VIVE", "ANDRE MARIE CARMEN BONI", "andre.mariecarmen@yahoo.fr", "22 BP 808 ABIDJAN 22", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT GIL", "ASSI ACHOU HYPPOLITE", "chicarmandine@gmail.com", "08 BP 111 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES ARTS", "AYENA EPSE COLOMB", "phciedesarts1@gmil.com", "08 BP 395 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES MIMOSAS", "BOBO LAURE EDWIGE", "akaedwige@hotmail.com", "25 BP 148 ABIDJAN 25", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRINCIPALE BINGER SARL", "GABA LEONCE", "gabaleonce@yahoo.com", "BP 27 BINGERVILLE", "ABIDJAN/BINGERVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CEPHAS", "KAKOU EPSE COMOE K.ELISE", "elisekakou64@yahoo.com", "22 BP 1353 ABIDJAN 22", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AMINA", "KOISSY KPEIN SERGE", "sergekoissykpein@yahoo.fr", "06 BP 635 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTS COME & DAMIEN", "KOUASSI JOCELYNE", "k.ajocelyne@yahoo.fr", "06 BP 2601 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVINE ONCTION", "LOROUX BI PATRICK ARMEL", "patrickloroux@gmail.com", "08 BP 2566 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE SAINT JOSEPH", "MOULOT YVES CHRISTIAN", "pharmaciesaintjoseph@aviso.ci", "01 BP 2051 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES 7 COLONNES", "SANLE ELLO YVONNE", "les7colonnes@gmail.com", "01 BP 7421 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT MARTIN DE FAYA", "SOUNAN KONI ELOYE GHISLAI", "seloyeghislain@yahoo.fr", "17 BP 530 ABIDJAN 17", "ABIDJAN/COCODY FAYA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PHLADELPHIE", "ADON JEAN JACQUES", "phciephiladelphie@gmail.com", "", ""));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EMMANUEL", "GBOGBOUO TRA GERMAINE", "georgetteg2000@yahoo.fr", "", ""));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA MAIRIE TREICH SARL", "KOUAKOU JEAN MAC ARGNAR", "agnarpharma@yahoo.fr", "16 BP 1538 ABIDJAN 16", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE DU PALAIS DES SPORT", "ADJA GNAMIEN ARNAUD C.", "nvellephciepds@yahoo.fr", "18 BP 136 ABIDJAN 18", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES CITES SYNACASSCI", "BONI HERVE", "boni_herve@yahoo.fr", "25 BP 293 CIDEX 1 ABIDJAN 25", "RIVIERA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE RIVIERA 3", "KANGA YAO FELIX", "phcieriviera3@gmail.com", "22 BP 48 ABIDJAN 22", "ABIDJAN/RIVIERA 3"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CITE COCODY ROUGE", "ASSAMOI TETCHI CLAUDE", "assamoitetchi58@yahoo.fr", "08 BP 2171 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE SAINTE ODILE", "NIAMKEY OLIVIER JOCELYN", "sainteodile@myorange.ci", "06 BP 2229 ABIDJAN 06", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MEYDEBA", "MEDEBA YOVOSSI EVELYNE", "evelyne_medeba@yahoo.fr", "BP 884 ANYAMA", "ANYAMA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES LAUREADES", "DESIREE PATRICIA BADIA", "ditybadia@yahoo.fr", "06 BP 1699 ABIDJAN 06", "ABIDJAN/ANGRE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BENABIE", "ACKAH BENIABIE", "ackah597@gmail.com", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'ENTENTE", "ARKURST M. BERNADETTE", "okabernadette@hotmail.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA PAIX", "DOUZOUA LUCIEN", "pharmaciedelapaix@live.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE VRIDI SIR", "EHOUNOUD MOULOT SIMONE", "phcievridisir@yahoo.fr", "", "ABIDJAN/VRIDI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LE TREFLE", "LASSEY DANIELLE", "danylassey1@yahoo.fr", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU SOLEIL", "SIBAILLY ARSENE", "PHCIEdusoleil@aviso.ci", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES ARCHANGES", "SANLE JEAN CRESORT", "phcie_arch@aviso.ci", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE REGINA", "COULIBALY NEE DODO REGINE", "ndjechia@gmail.com", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA BIA", "DINDJI JULES", "dindjijules@aviso.ci", "", "ABOISSO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE THERESE SARL", "AYEPA GEORGES", "ayepa@hotmail.com", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA GALERIE SARL", "KACOU AIME ARSENE", "kacouedoua@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE WILLIAMSVILLE SARL", "KEHIRI LYDIE", "lydie-kehiri@live.fr", "", "ABIDJAN/ADJAME-WILLIAMSV"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES ENTREPRISES", "ASSAMOI ALFRED", "alfred.assamoi@yahoo.fr", "", "ABIDJAN/VRIDI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FLORA", "ANOH ROSE ARMANDE", "anohrose@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CRHIST'EMILE", "KOUASSI ERIKA EPSE AKA", "erickaclaude@yahoo.fr", "", "KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PROVIDENCE /ABENGOUROU", "OUATTARA MAMADOU", "maderz77@yahoo.fr", "BP 574 ABENGOUROU", "ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AKADJOBA", "AGOUSSI MARIE-GERMAIN", "dr_agoussi@yahoo.fr", "04 BP 1747 ABIDJAN 04", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVINE ESPERANCE SARL U", "APATA PELAGIE EPSE BOGLO", "pelagieb@yahoo.fr", "14 BP 1067 ABIDJAN 14", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SANDRINA", "BECHIE N'GUESSAN", "bechieyapo@yahh.fr", "01 BP 3769 ABIDJAN 01", "YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA PROVIDENCE ADJAME", "BEDI MEL SAMUEL", "", "09 BP 1491 ABIDJAN 09", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PORT-BOUET II SARL", "EDOUA KACOU JACQUELINE", "mamyjacquie@yahoo.fr", "06 BP 880 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ARTEMIA", "EFFI N'DOUA ESTHER", "artemia@aviso.ci", "BP 806 CEDEX 3 ABIDJAN 08", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LOCODJORO SARL", "ESSO SERGE DAVY", "essodavy@yahoo.fr", "23 BP 672 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ANANERAIE SUARL", "GALLE ATTABLI ANSELME", "galleatabli22@yahoo.fr", "BP YOPOUGON", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LAGOMA", "GBATO WAKELL FRANCIS", "gbatofrancis@yahoo.fr", "25 BP 52 ABIDJAN 25", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU PROGRES", "KACOU BEUGRE ELISABETH", "eblkacou@hotmail.com", "02 BP 750 ABIDJAN 02", "ABIDJAN/WILLIAMSVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES GARES SARL", "KIRE KOUADIO NOEL", "kirenoel@yahoo.fr", "02 BP 153 ABIDJAN 02", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU BANCO SARL", "KONE DRAMANE", "fanthakone@yahoo.fr", "03 BP 654 ABIDJAN 03", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE SION", "KORE LEKADOU SYLVIE NOELL", "PHCIEdesion11@gmail.com", "01 BP 4139 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE JEAN-PIERRE", "KOUA TANOH ROGER", "PHCIE.jeanpierre@yahoo.fr", "BP", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT ANDRE SARL U", "KONE LANDOLO ALPHONSE", "landelor1@yahoo.fr", "21 BP 825 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE LEM", "KOUAKOU KOFFI", "kkoffi69@yahoo.fr", "23 BP 1123 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES ECLUSES SARLU", "KOUASSI ATTA RAOUL", "attaraoul@yahoo.fr", "08 BP 2014 ABIDJAN 08", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SANTA CATERINA", "KOUASSI BRUNO ABRAHAM", "kouassibruno25@yahoo.fr", "22 BP 152 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AIMESS", "KOUASSI ESSO JEANNETTE F.", "ph_aimess276@yahoo.fr", "20 BP 582 ABIDJAN 20", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE QUARTIER EBRIER SARL", "LAMBIN CHRISTIAN", "phcieebrie@yahoo.fr", "01 BP 121 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE TOIT ROUGE SUARL", "LASME DIOMANDE HERMANCE", "hermlas@yahoo.fr", "25 BP 1084 ABIDJAN 25", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MAI", "MAI CLAUDE & KOUASSI EUGE", "", "BP 76 AGNIBILEKROU", "AGNIBILEKROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT EMMANUEL", "NOGBOU DIDIER", "maguinobou@yahoo.fr", "06 BP 2818 ABIDJAN 06", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE VALORS", "ORSOT BOSSO VALENTIN", "orsotvalentin@yahoo.fr", "21 BP 643 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT MICHEL", "SIBAILLY ASSAMOI LOUISE", "stmichelpharma@gmail.com", "03 BP 425 ABIDJAN 03", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DESBRONS", "SOROHO GBARAMA", "sorohogbarama@hotmail.com", "BP 97 TANDA", "TANDA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SCHEKINA - YOP", "TCHUERE ALEXIS", "hblessoue@yahoo.fr", "21 BP 5259 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST ANGE EMMANUEL", "THOBOUET SANDRINE CRITIE", "phciestangeemmanuel@gmail.com", "03 BP 270 ABIDJAN 03", "ABIDJAN/YOPOUGON SOGEFIHA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TIZRA", "TIE BI ZAMBLE DENIS", "", "23 BP 3564 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MARCHE DABOU SARL-U", "TOURE BABACAR", "dr_toure66@yahoo.fr", "BP 364 DABOU", "DABOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE AWALE (RACHAT)", "WANDJI ZAROU BORIS CLOVIS", "wandjiboris@yahoo.fr", "23 BP 2992 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE CHOQUET", "ZOGUEI DAHAHON B.OLGA CHR", "olgazog2004@yahoo.fr", "BP 63 BONDOUKOU", "BONDOUKOU"));

        // Liste supplémentaire de pharmacies
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERNE-MAZUET", "KOUASSI PARFAIT-SCHAUER Y", "phciemazuet@aviso.ci", "01 BP 167 ABIDJAN 01", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MAZUET SPECIAL", "KOUASSI PARFAIT", "phciemazuet@aviso.ci", "01 BP 167 ABIDJAN 01", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES CITES SYNACASSCI", "BONI HERVE", "boni_herve@yahoo.fr", "25 BP 293 CIDEX 1 ABIDJAN 25", "RIVIERA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE DE LA ME", "N'DOUFFOU JOEL DON", "ndouffoujoel@yahoo.fr", "13 BP 1278 ABIDJAN 13", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES FINANCES", "ANGOUA TANOE EMMA CLARISS", "emmaangoua@yahoo.fr", "15 BP 265 ABIDJAN 15", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARCORY PTT CESSION", "KAMEL SAYEGH", "nouvellePHCIEptt@gmail.com", "05 BP 1600 ABIDJAN 05", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PALMERAIE SARL", "M'BAHIA YAO KOUAME HONORE", "palmbahia@yahoo.fr", "06 BP 6091 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES DEUX PLATEAUX SARL", "PIOT VLINDET ALINE", "pharm2pltx@yahoo.com", "06 BP 1036 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE RIVIERA 3", "KANGA YAO FELIX", "PHCIEriviera3@gmail.com", "22 BP 48 ABIDJAN 22", "ABIDJAN/RIVIERA 3"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ATLANTIQUE", "OUATTARA KANIGUI", "kanigui@yahoo.com", "02 BP 219 ABIDJAN 02", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES HALLES-SOCOCE", "KIE DANIELLE", "phciedeshalles@yahoo.fr", "22 BP 506 ABIDJAN 22", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE DU PALAIS DES SPORT", "ADJA GNAMIEN ARNAUD C.", "nvellephciepds@yahoo.fr", "18 BP 136 ABIDJAN 18", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES LAGUNES", "AJAMI TAREK AKIL", "phiedeslagunes@yahoo.fr", "05 BP 661 ABIDJAN 05", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU LYCEE TECHNIQUE", "DIAWARA IBRAHIM", "pharma_tech@aviso.ci", "01 BP 6578 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE & LABO. LONGCHAMP", "PITTE BEUGRE ALBERT", "apitte@yahoo.fr", "01 BP 11 ABIDJAN 01", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE AVENUE NOGUES", "FAWAZ KARIM", "karimfawaz5@hotmail.com", "03 BP 1881 ABIDJAN 01", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BALNEAIRE", "MOTTO ARMAND", "phcie.balneaire@yahoo.fr", "12 BP 2192 ABIDJAN 12", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU ROND POINT CHU", "BLEU LAINE SIMONE", "phciedurondpoint@aviso.com", "18 BP 540 ABIDJAN 18", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRANDE PHCIE DU COMMERCE-PLT", "MANGOUA GERARD", "gpc@aviso.ci", "01 BP 1007 ABIDJAN 01", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BEL AIR", "DOUKOURE DOMINIQUE", "phcie_belair@yahoo.fr", "01 BP 10297 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KENEYA", "DOUKOURE MOUSTAPHA", "keneyap@hotmail.fr", "01 BP 531 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE RUTH", "BOUBOUTOU KABORE LINDA", "linda@kabore.com", "05 BP 126 ABIDJAN 05", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME", "AKA ANGHUI HORTENSE", "phandame@yahoo.fr", "01 BP 2430 ABIDJAN 01", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA MAIRIE TREICH SARL", "KOUAKOU JEAN MAC ARGNAR", "", "16 BP 1538 ABIDJAN 16", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EBENEZER", "AMANI ANTHELME ENOC", "pharmacie.ebenezer@gmail.com", "23 B P 3811 ABIDJAN 23", "BONOUA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MANZAN", "ATTOBRA A.ELISABETH", "attobraliz@hotmail.com", "13 BP 1357 ABIDJAN 13", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CHEMINOT", "DA LEONCE BRUNO", "phieducheminot@yahoo.fr", "05 BP 1263 ABIDJAN 05", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE MARTHE", "ZADI AGOH MARTHE", "ettie_jb2011@yahoo.fr", "BP 506 CIDEX 1 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU ZANZAN", "AFFRE KOUASSI FELIX", "affrekouassifelix@yahoo.fr", "BP 568 BONDOUKOU", "BONDOUKOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("CIRBA-MME DOUMATEY SYLVIE", "CIRBA - MME DOUMATEY SYLV", "", "18 BP 2071 ABIDJAN 18", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LATRILLE 2 PLATEAUX", "DANO PATRICIA", "phlatrille@afnet.net", "08 BP 1812 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE MATENE", "TRAORE MATENIN", "PHCIEmatene@yahoo.fr", "BP 828 CIDEX 1 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MONASTERE", "KOUAME K. CHRISTOPHE", "PHCIEmonastere@gmail.com", "06 BP 1423 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'ETOILE SARL.U", "MANOUAN AKA YOLANDE", "etoilePHCIE@yahoo.fr", "06 BP 55 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NANKOKO", "AFFOLABY COUASSI-BLE ANNE", "PHCIEnankoko@yahoo.fr", "17 BP 489 ABIDJAN 17", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE AGATHE", "NEBAVI N'GUESSAN FRANCOIS", "steagathe@hotmail.com", "10 BP 874 ABIDJAN 10", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BELLE CITE", "ANOUGBLE N'ZOUE MARIETTE", "anougblemariette@yahoo.fr", "13 BP 3092 ABIDJAN 13", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AVOCATIER MARCHE SARL", "DIOP KOUAME JUSTE MATHURI", "diopkouame@yahoo.fr", "14 BP 42 ABIDJAN 14", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CITE COCODY ROUGE", "ASSAMOI TETCHI CLAUDE", "cocodyPHCIEcite@yahoo.fr", "08 BP 2171 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CENACLE", "BROU ARSENE", "phcieducenacle@yahoo.fr", "21 BP 972 ABJ-01 BP 1305 ABJ", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU WHARF SARL", "ATTEY ARMELLE EPSE SEKA", "phwharf@aviso.ci", "12 BP 2358 ABIDJAN 12", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE WILLIAM PONTY SARL", "SOTTY KOFFI GERVAIS", "gervaisvino@yahoo.fr", "21 BP 2814 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT-ATHANASE", "ALLAH KOUADIO ANNE-MARIE", "avoakann@yahoo.fr", "25 BP 988 ABIDJAN 05", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EDEN SARL.U", "KONAN STEPHANE ADJOUA", "PHCIEdenbonoua@aviso.ci", "BP 771 BONOUA", "BONOUA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE SAINTE ODILE", "NIAMKEY OLIVIER JOCELYN", "sainteodile@myorange.ci", "06 BP 2229 ABIDJAN 06", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE WAKOUBOUE", "AHIPO BABO GILLES STANIS", "pharmwak@yahoo.fr", "05 BP 661 ABIDJAN 05", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MEYDEBA", "MEDEBA YOVOSSI EVELYNE", "evelyne_medeba@yahoo.fr", "BP 884 ANYAMA", "ANYAMA"));

        // Liste supplémentaire de pharmacies
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KORALIE", "KOKORA GUILLAUME", "guillaumekokora@pharmaciekoralie.com", "01 BP 736 ABIDJAN 01", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES HIBISCUS", "ADOU MARIE LOUISE", "nouvellePHCIEdeshibiscus@yahoo.fr", "11 BP 659 ABIDJAN 11", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA MAIRIE ABOBO", "EBY EHOUNOUD", "", "06 BP 1275 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE MARIE DES BEATITU", "LAMINE LACHIROY", "pharmarib210205@yahoo.fr", "06 BP 2125 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARTHE ROBIN", "ASSOUAKON JEAN-CLAUDE", "phciemartherobin@yahoo.fr", "25 BP 1240 ABIDJAN", "ABIDJAN/RIVIERA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE MARIE-THERESE", "NOGBOU AKRE CLAUDE", "gbakrag@yahoo.fr", "10 BP 1233 ABIDJAN 10", "ANIASSUE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT MARTIN S.A.R.L", "ESSOH-ESSIS EP.KAKOU M.-P", "mpkesso@gmail.com", "08 BP 1034 ABIDJAN 08", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU FORUM", "BOGUIFO DIRABOU CHARLES", "boguifocha@gmail.com", "04 BP 1668 ABIDJAN", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU GRD. MARCHE MARCORY", "GOUEPO EVARISTE", "evaristegouepo@yahoo.fr", "MARCORY", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT JOSEPH", "AKPO MARIE LOUISE", "", "BP 285 TIASSALE", "TIASSALE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES OLIVIERS", "MANGOUA JOSE JULES", "PHCIElesoliviers@yahoo.fr", "22 BP 1470 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SEBROKO SARL", "GBOCHO SOSTHENE EDGAR", "phciesebrokosarl@yahoo.fr", "06 BP 1541 ABIDJAN 06", "ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU RAIL", "TCHIMOU LUCIEN", "PHCIE_du_rail@yahoo.fr", "01 BP 11855 ABIDJAN 01", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA MAIRIE D'ADJAME", "KOFFI SYLVIA EPSE YANGNI", "pmairie14@gmail.com", "01 BP 1863 ABIDJAN 01", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MATY", "TRAORE AISSATA DESIREE", "aissatadtraore@hotmail.com", "23 BP 2185 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE COLOMBE/BASSAM", "AGO KOUAME", "agokouame@gmail.com", "BP 138 GRAND-BASSAM", "GRAND-BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE D'AFFERY", "AZAGOH KOUADIO GUILLAUME", "nvllephciedaffery@yahoo.fr", "BP 198 AKOUPE", "AKOUPE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES 4 SAISONS", "BADINI MARIAME", "mariamebadini@yahoo.fr", "10 BP 3012 ABIDJAN 10", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SACRE COEUR", "ATTIA MARIE-LAURENCE A.", "attialaurence@yahoo.fr", "22 BP 1747 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GALILEE", "GNALY OTRO MOISE", "PHCIEgalilee20@gmail.com", "28 BP 150 ABIDJAN 28", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE IMANN", "DOUMBIA MARIAM CISSOKO", "phcie.imann@aviso.ci", "18 BP 1975 ABIDJAN 18", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE DU ROCHER", "M'BRO MELEM SYLVIE", "jeanjacquesadon@yahoo.fr", "01 BP 284 ABIDJAN 01", "ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE RITA DE CASCIA", "GUIDY EUGENIE CYNTHIA", "phciesrc@yahoo.fr", "BP 323 CIDEX 03", "YOPOUGON OUEST"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ZONE MICAO", "VANGAH MALAN ABOH HENRIET", "henrie03@yahoo.com", "17 BP 124 ABIDJAN 17", "YOPOUGON EST"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT JOSEPH ARTISAN", "KOUAKOU IVAN BEKAN-N'THY", "bekanthy@yahoo.fr", "01 BP 1669 ABIDJAN 01", "KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT VIATEUR", "KISSIEDOU KOPOIN RACHELLE", "dr.kissiedou@yahoo.fr", "28 BP 1460 ABIDJAN 28", "ABIDJAN/COCODY RIVIERA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE CECILE", "BROU CAROLINE", "brou.carolin@gmail.com", "17 BP 536 ABIDJAN 17", "ABIDJAN/COCODY II PLATEAUX"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT AUGUSTE", "DIE KOUADIO KOUAKOU GEORG", "diegeorges@yahoo.fr", "04 BP 78 ABIDJAN 04", "KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHU TREICHVILLE", "AMICHIA MAGLOIRE", "amichia01@yahoo.fr", "BP V3 ABIDJAN", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MAGNIFICAT SARL", "LOBA BOYO MARTIN", "gaddahelodie@gmail.com", "28 BP 1411 ABIDJAN 28", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE YEM-BLA", "KAO CELESTINE", "", "BP 20633 AV.AKEI N°258", "LOME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ABYA", "COULIBALY SARAGATIO ASSET", "astucool75@yahoo.fr", "26 BP 180 ABIDJAN 06", "KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE NISSI", "SIKA KOUASSI CHARLES", "phcienissi@hotmail.fr", "25 BP 318 CIDEX01 ABIDJAN", "BINGERVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE JERICHO", "TRAZIE ZORAUD ALAIN", "alainzoraud@yahoo.fr", "01 BP 10019 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRDE PHCIE SAINT MOISE SARL", "KOUAME TIKORI ROGER", "kouametikoriroger@yahoo.fr", "30 BP 442 ABIDJAN 30", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUR D'ABOBO", "KONE MAIMOUNA BEBE", "dr.konemaimouna.b@gmail.com", "01 BP 1305 ABIDJAN 01", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AZUR", "CHINTOH ARMSTRONG PIERRE", "pachintoh@gmail.com", "01 BP 6145 ABIDJAN 01", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KENNEDY", "KASSI DANHO JULIETTE", "julied.kassi@gmail.com", "06 BP 1445 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA SOGEFIHA", "DJEREHE RICHARD", "pharmasogefiha@yahoo.fr", "14 BP 344 ABIDJAN 14", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRANDE PHCIE DU DOKUI", "DOUMBIA MOHAMED", "mdoumpharma@gmail.com", "06 BP 2224 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA CITE ABOBO", "EGNANKOU JULIETTE", "", "14 BP 03 ABIDJAN 14", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CITE MAROC SARL U", "HAMZA MOHAMED TAHAR", "mohamed.hamza6610@gmail.com", "06 BP 2650 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MEGANE", "BONNY OLIVIER", "PHCIEmegane@yahoo.com", "26 BP 75 ABIDJAN 26", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE YARAPHA SARL", "ACKRA KOUAKOU NOEL", "yarapha@gmail.com", "13 BP 2967 ABIDJAN 13", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES LAUREADES", "DESIREE PATRICIA BADIA", "ditybadia@yahoo.fr", "06 BP 1699 ABIDJAN 06", "ABIDJAN/ANGRE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE ELIEL", "ATSE ASSI RODRIGUE", "nouvellephcieliel@yahoo.fr", "BP 1003 ANYAMA", "ABIDJAN/ANYAMA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR ANADOR", "ADJAFFI N'DOUA JEAN-NOEL", "jeannoeladjaffi@yahoo.fr", "18 BP 1916 ABIDJAN 18", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'OASIS DE PAIX", "FOUA-BI ESTHER URSULE", "estherfouabi@yahoo.fr", "09 BP 204 ABIDJAN 09", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CATHERINE DE SAMO", "BALECOIDJO VERONIQUE I.A.", "pharmakre@gmail.com", "06 BP 1989 ABIDJAN 06", "BONOUA/GRAND-BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU PALAIS DE LA CULTURE", "SEKERE KOUADIO AMBROISE", "sekere@raphasante.net", "06 BP 6912 ABIDJAN 06", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE DE JACQUEVILLE", "ASSA EPSE ANKEMAN", "nvlphciejqville@yahoo.fr", "BP 313", "ABIDJAN/JACQUEVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE WASSAKARA CESSION", "NIAMKE KOKO MARIE LAURE", "mlniamke@yahoo.com", "17 BP 545 ABIDJAN 17", "YOPOUGON OUEST"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHRIST MARIE DE PAILLET", "MOBIO KOFFI NESTOR", "mobio1005@gmail.com", "09 BP 4262 ABIDJAN 09", "ADJAME"));

        // Liste supplémentaire de pharmacies
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE J.LOG SARL", "LOROUGNON N'ZI DESIREE", "pharmaciejlog@gmail.com", "22 BP 297 ABIDJAN 22", "GD BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME DE LA VISITAT", "ACHO SYLVAIN", "sylvain.acho@yahoo.fr", "01 BP 1535 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT BERNARD SARL", "ADOUKO AKA AMENAN MARIE", "marieadou2006@yahoo.fr", "28 BP 1022 ABIDJAN 28", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES ALLEES UARL", "AHNOUX NIAHNKON EHKONG M.", "PHCIEdesalleesabidjan@yahoo.fr", "06 BP 6979 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES ARUMS SARLU", "AIME DIMBOUR", "dimbouraime@yahoo.fr", "06 BP 1727 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA RIVIERA", "AKOBE NANOU C-KOUAMELAN C", "christmelan@yahoo.fr", "08 BP 812 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BON PASTEUR SARL", "AKOBE-NANOU CATHERINE", "phcienabenou@yahoo.fr", "25 BP 585 ABIDJAN 25", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AKRE ALBERT ASSAMOI", "AKRE LILIANE ARLETTE", "pharmakre@gmail.com", "01 BP 7203 ABIDHJAN 01", "ABIDJAN/BINGERVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BARAKA", "AMANHO DJININ HYACINTHE", "adedouin@yahoo.fr", "13 BP 1036 ABIDJAN 13", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT GABRIEL", "AMON YOLANDE", "yolandeatt@hotmail.fr", "06 BP 2287 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EAU VIVE", "ANDRE MARIE CARMEN BONI", "andre.mariecarmen@yahoo.fr", "22 BP 808 ABIDJAN 22", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CYD NIKO DES LAURIERS", "ANON EPSE AMIN AGUIE RENE", "amin_anon@yahoo.fr", "08 BP 2782 ABIDJAN 08", "ABIDJAN/COCODY/LES LAURIERS"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MONT CARMEL", "ASSAMOI EPSE YEO AYA JAC", "phciemontcarmel@yahoo.fr", "28 BP 620 ABIDJAN 28", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT GIL", "ASSI ACHOU HYPPOLITE", "chicarmandine@gmail,com", "08 BP 111 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES ARTS", "AYENA EPSE COLOMB", "phciedesarts1@gmil.com", "08 BP 395 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT JOSEPH SARL 2PLTX", "BONI HENRI & ASSI M.PAULE", "phciestjoseph@gmail.com", "25 BP 602 ABIDJAN 25", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU BLD DE FRANCE", "BROU KOUASSI FIRMIN", "phcie.bvd2france@yahoo.fr", "06 BP 2498 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST FRANCOIS DE DANGAH SA", "CARRETTE OUEGNIN ELISA", "carretteelisabeth@yahoo.fr", "06 BP 222 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU VALLON", "COULIBALY MAYELE", "coulibalymayele@yahoo.fr", "06 BP 88 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT JEAN SARL", "CREPPY FRANCINE", "fkcreppy@yahoo.fr", "08 BP 34 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE FAMILLE", "DIAKITE SERIE MARIE CECIL", "mcs_diak@hotmail.com", "06 BP 1300 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE D'ANGRE", "KOFFI AISSATOU EP.KONIAN", "koffiaissatou@gmail.com", "06 BP 104 CIDEX 1 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE AGATHE", "KONE DETOH ISABELLE", "isoukone@hotmail.com", "25 BP 1498 ABIDJAN 25", "ABIDJAN/RIVIERA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE YANDI", "KONE LADJI", "koneladji22@gmail.com", "01 BP 217 ABIDJAN 01", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ESPACE SANTE", "KOUAME MARIE-ANGE", "PHCIEespacesante@yahoo.fr", "06 BP 2869 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVINE ONCTION", "LOROUX BI PATRICK ARMEL", "patrickloroux@gmail.com", "08 BP 2566 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE COCODY SARL", "MAREGA MAIMOUNA", "maregaba@aviso.ci", "08 BP 461 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PH NOTR DAM D L'INCARNAT.SARL", "SANGA NEE EKRA NANAN BASI", "sangabasilide@yahoo.fr", "30 BP 535 ABIDJAN 30", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ARC-EN-CIEL SARL.U", "TOURE SAPHIATOU KOFFI", "arcencielPHCIE@yahoo.fr", "06 BP 435 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PHLADELPHIE", "ADON JEAN JACQUES", "phciephiladelphie@gmail.com", "", ""));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES JARDINS D'EDEN", "ABOA AGUIMA EPSE KANGA J.", "jkangaaboa@yahoo.fr", "18 BP 2313 ABIDJAN", "ABIDJAN/RIVIERA 3"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE ANNE", "ABOLE SOPIE ANGE BRIGITE", "angebrigitte19@yahoo.fr", "12 BP 2192 ABIDJAN 12", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LE REEL", "AFFRO EPSE EROUBINOU AYA", "evelyneroubinou@yahoo.fr", "BP 690 GRAND-BASSAM", "GRAND BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHATEAU D'EAU D'ANGRE", "AGOH DIAWARA CLEMENCE", "PHCIEchateaudeau@gmail.com", "27 BP 371 ABIDJAN 27", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE D'ABOBOTE", "AKE AMAFIN NICODEME", "nake906@gmail.com", "14 BP 1280 ABIDJAN 14", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BELLE VUE D'ANGRE", "AMANY MONVO FABIENNE", "fabiamany@yahoo.fr", "01 BP 1953 ABIDJAN 01", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU GOLF SARL-U", "AMICHIA ABA VICTOIRE", "amichiavictoire@yahoo.fr", "25 BP 2069 ABIDJAN 25", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE VAL DE GRACE", "AMICHIA PAUL INNOCENT", "PHCIEn@PHCIEvdg.com", "BP 86 CIDEX 03 ABIDJAN", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE BLOCKHAUSS SARLU", "ANONGBA EDWIGE", "phcieblockhauss@yahoo.fr", "27 BP 717 ABIDJAN 27", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EMMANUEL", "ASSOMA MICHEL HENRI", "mhassoma@yahoo.fr", "06 BP 6279 ABIDJAN 06", "ABIDJAN/II PLTX 8EM TRANCHE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ACACIA", "ATSE JEAN-PHILIPPE", "jp.atse@akwaci.com", "06 BP 16 ABIDJAN 06", "COCODY/ANGRE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA PIETE", "BAMBA SOUMAH JOSEPHINE", "", "06 BP 288 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOURAM SARL", "BAMBA ZOUMANA", "phcienouram@live.fr", "06 BP 2636 ABIDJAN 06", "ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TERA", "BARRY EPSE KANTE AICHATOU", "chatouka@hotmail.com", "06 BP 1421 ABIDJAN", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE ABOBO BELLEVILLE SA", "BEUGRE GUY CLAVER", "beugre.guy13@live.fr", "27 BP 231 ABIDJAN 27", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES MIMOSAS", "BOBO LAURE EDWIGE", "akaedwige@hotmail.com", "25 BP 148 ABIDJAN 25", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT ALEXANDRE", "BOKRA EPSE OULAYE REGINE", "", "22 BP 1653 ABIDJAN 22", "ABIDJAN/ABOBO PK8"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLLE PHCIE MIRIA", "BOSSE EPSE GADDAR JULIANA", "PHCIEmiria@gmail.com", "01 BP 120 ABIDJAN 01", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LIVIE", "BOUABRE EDWIGE ANNICK", "PHCIElivie@gmail.com", "01 BP 3343 ABIDJAN 01", "AKUEDO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES TULIPES", "COULIBALY ASSANE", "PHCIEtulipes@aviso.ci", "06 BP 6537 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA PLAQUE II", "COULIBALY BERTHE YEKELEYA", "phcieplaq2@yahoo.fr", "03 BP 1149 ABIDJAN 03", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BEL HORIZON", "COULIBALY FATOUMATA", "afa_7@yahoo.fr", "22 BP 1134 ABIDJAN 22", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE OLYMPIQUE", "COULIBALY KAFANA DANIEL", "kafanad@yahoo.fr", "01 BP 7660 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TIMA", "COULIBALY YESSORO DANIEL", "pharmatima1@gmail.com", "06 BP 88 ABIDJAN 06", "ABIDJAN/ABOBO-GARE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE BEATRICE", "COWPPLI-BONY P", "patrick.cony@gmail.com", "17 BP 509 ABIDJAN 17", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MARCHE D'ANYAMA", "DAH OLGA MOUSSOKORO", "mousso_koro@yahoo.fr", "27 BP 651 ABIDJAN 27", "ANYAMA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PETIT MARCHE DU DOKOUI", "DALLO GUILLAUME", "guillaumedallo@yahoo.fr", "26 BP 19 ABIDJAN 26", "ABIDJAN/COCODY PLTX DOKUI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA DJIBI SARL", "DEMPAH MALAN BRIGITTE", "PHCIEdeladjibi@yahoo.com", "06 BP 947 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE CROIX", "DEROUX VICTOR", "PHCIEsaintecroix@yahoo.fr", "14 BP 799 ABIDJAN 14", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LAS PALMAS SARLU", "DIAKITE FATOUMATA", "phcielaspalmas@aviso.ci", "27 BP 944 ABIDJAN 27", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CHATEAU", "DIAKITE IBRAHIM", "ibrahimdia@yahoo.fr", "BP 906 ANYAMA", "ANYAMA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EMERAUDE", "DIARRA ISMAIL SAMBA", "isojovi@yahoo.fr", "01 BP 2930 ABIDJAN 01", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ROUTE D'ANYAMA", "DIE OFFEY JUSTE", "die.juste@yahoo.fr", "01 BP 13291 ABIDJAN 01", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES ELIAS", "DIOUMENCY EP.EMMOU ANGELE", "phcieleselias@yahoo.fr", "25 BP 995 ABIDJAN 25", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST PIERRE D'ANONO SARL", "ENOH SOMIAN GUY SERGE", "enoh@aviso.ci", "28 BP 123 ABIDJAN 28", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PARAPHARMACIE DU MALL", "FAKIH CHADI", "parapharmaciedumall225@gmail.com", "18 BP 811 ABIDJAN 18", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LA PROVIDENCE SARL", "FOADEY KOKOE JOCELYNE DOH", "ph_providence@yahoo.fr", "13 BP 1137 ABIDJAN 13", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRINCIPALE BINGER SARL", "GABA LEONCE", "gabaleonce@yahoo.com", "BP 27 BINGERVILLE", "ABIDJAN/BINGERVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRINCIPALE BINGERVILLE", "GABA LEONCE", "gabaleonce@yahoo.com", "BP 27 BINGERVILLE", "BINGERVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT RAPHAEL ARCHANGE", "GABLA LILIANE TOUTOUKPE", "PHCIEstraphaelarchange@yahoo.fr", "08 BP 830 ABIDJAN 08", "ABIDJAN/RIVIERA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MAGNIFICAT DES II PLTX", "GADDAH KOUASSI SYLVERE E.", "gaddahelodie@gmail.com", "06 BP 6605 ABIDJAN", "Deux Plateaux"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE RUE MINISTRE", "GASSAUD MARCEL", "rueministre@yahoo.fr", "09 BP 1193 ABIDJAN 09", "ABIDJAN/RIVIERA PALMERAIE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BOAZ", "KABLAN EPSE OPOUMBA N'DAF", "mcopoumba@yahoo.fr", "08 BP 3616 ABIDJAN 08", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT CHRISTOPHE", "KADJO AMICHIA ANGELE", "pcie.stchristophe@yahoo.fr", "06 BP 6756 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CEPHAS", "KAKOU EPSE COMOE K.ELISE", "elisekakou64@yahoo.com", "22 BP 1353 ABIDJAN 22", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE D'ANYAMA SARL", "KEBE SEREBA", "pharmaciekebe@gmail.com", "BP 495 ANYAMA", "ANYAMA"));

        // Liste supplémentaire de pharmacies
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BEREKYAH", "KOFFI YAPI M.EPSE GNAYORO", "maryse_ci@yahoo.fr", "08 BP 186 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AMINA", "KOISSY KPEIN SERGE", "sergekoissykpein@yahoo.fr", "06 BP 635 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT-PAUL", "KOMOE KADJO ANSELME", "phaciestpaul@yahoo.fr", "22 BP 669 ABIDJAN 22", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES JARDINS", "KONAN AKISSI MARIE JEANNE", "PHCIEdesjardins@aviso.ci", "BP 438 CIDEX 1 ABIDJAN", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MARCHE BINGVIL SARL.U", "KONAN KOUAME", "marchebingerville@yahoo.fr", "BP 73 BINGERVILLE", "BINGERVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LUMINANCE K", "KONAN NIAMIEN YVETTE", "luminancek@gmail.com", "01 BP 12814 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE BONOUMIN/ASSISTANCE", "KONAN YAO JACQUES", "jkonanyao@yahoo.fr", "09 BP 2852 ABIDJAN 09", "ABIJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ACTUELLE", "KONE LASSINA", "lasskone@yahoo.fr", "08 BP 1309 ABIDJAN 08", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ELIEZER", "KONE MOUGUISSI PHILOMENE", "koneph1@yahoo.fr", "01 BP 11865 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DUNIA", "KONE SORO FATOU", "PHCIEdunia@gmail.com", "08 BP 1008 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE RAHMAN", "KONE YAYA", "yayebill@yahoo.fr", "13 BP 388 ABIDJAN 13", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT PIERRE DES ROSEES", "KOUADIO KOKORA", "stpierrerosees@aviso.ci", "03 BP 331 ABIDJAN 03", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOEMIE SARL", "KOUAME ANON & ANON ARSENE", "anonarsene@yahoo.fr", "01 BP 5415 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KANN'SY", "KOUAME ASSACY K.NATHALIE", "assacnath@gmail.com", "01 BP 1305 ABIDJAN 01", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE  SAINT SAUVEUR", "KOUAME DIDIER KONAN", "teagerard@gmail.com", "13 BP 833 ABIDJAN 13", "ABIDJAN / ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ABOBO SANTE", "KOUAME HOMAWOO CHARLES", "homawoo9@hotmail.com", "13 BP 1109 ABIDJAN 13", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'IMMACULEE CONCEPTIO", "KOUAME JEAN-CLAUDE KONAN", "phimmaculeeconception@aviso.ci", "25 BP 1019 ABIDJAN 25", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTS COME & DAMIEN", "KOUASSI JOCELYNE", "k.ajocelyne@yahoo.fr", "06 BP 2601 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE HARMONY", "KOUASSI N'GOTTA BEATRICE", "pharmaciesteharmony@live.fr", "06 BP 6629 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA CORNICHE", "KOUATELAY DABOIKO VIVIANE", "manouan@afnet.net", "01 BP 2197 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ANDY", "LARTEY M.THERESE EP.ALLAH", "larteymt@yahoo.fr", "08 BP 1667 ABIDJAN 08", "ABIDJAN/RIVIERA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE HERMES", "M'BASSIDJE K. BERNARD", "bernard.mbassidje@ymail.com", "06 BP 358 ABIDJAN 06", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME FATIMA(DOKUI)", "MESSE PAULE-ANGELE", "Phndfatima@gmail.com", "13 BP 2035 ABIDJAN 13", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE IRYS", "MESSOU KRAIDI MARIE", "m.mericick@yahoo.fr", "10 BP 955 ABIDJAN 10", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE SAINT JOSEPH", "MOULOT YVES CHRISTIAN", "pharmaciesaintjoseph@aviso.ci", "01 BP 2051 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST. SYLVESTRE", "M'POUE YAVO PIERRE CARLOS", "sylpharma1@yahoo.fr", "BP 303 BINGERVILLE", "ABIDJAN/BINGERVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BOULEVARD MITTERAND", "N'DOUBA KASSI RAPHAEL", "", "25 BP 976 ABIDJAN 25", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR SANTE", "N'DRI ANGELE EPSE KOUASSI", "ndriangelaure@yahoo.fr", "BP 417 BINGERVILLE", "ABIDJAN/BINGERVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE N'GOHESSE", "N'GOHESSE PARFAIT", "ngohesse@yahoo.fr", "01 BP 1003 ABIDJAN 01", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AURORE", "N'GOUIN CLAIH KOUADIO S", "phcieaurore@gmail.com", "06 BP 1710 ABIDJAN 06", "ABIDJAN/COCODY RIVIERA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR N'DOTRE", "OGOUMOND CHIMENE TANO", "chimenetano@yahoo.fr", "12 BP 684 ABIDJAN 12", "N'DOTRE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BOAZ", "OPOUMBA N'DAFFO M.CHANTAL", "mcopoumba@yahoo.fr", "08 BP 3616 ABIDJAN 08", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE RESIDENCE ABOBO (RAB PK", "OUATTARA SALIMATA", "sal_bam@yahoo.fr", "06 BP 2611 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE CENTRE (ABOBO)", "OUATTARA YACOUBA MARIUS", "phieducentre@yahoo.fr", "21 BP 2215 ABIDJAN 21", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE MARIE - COCODY SAR", "OURA ACHILLE MONIN", "achille.oura@gmail.com", "25 BP 165 ABIDJAN 25", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRINCIPALE ABOBO-TE SARL", "OURAGA NEE KOUAME LINDA A", "nppabobote@gmail.com", "25 BP 18 ABIDJAN 25", "ABIDJAN/ABOBO-GARE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ROUX", "ROUX HUBERT", "", "06 BP 339 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT MARTIN DE FAYA", "SOUNAN KONI ELOYE GHISLAI", "pharmastefoi@yahoo.fr", "17 BP 530 ABIDJAN 17", "ABIDJAN/COCODY FAYA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE M'POUTO", "SOUNDELE GNOLI CLEMENT", "soukyatehe@yahoo.fr", "06 BP 1799 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BSA", "SYLLA CHEICK OUMAR", "m_soumahoro2002@yahoo.fr", "06 BP 2803 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRANDE PHCIE DU JUBILE", "TAI NINSEMON BRICE", "tahianin1@yahoo.fr", "22 BP 219 ABIDJAN 22", "ABIDJAN COCODY ANGRE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE II PLATEAUX-AGBAN", "TIA ANNICK EMMA", "tianedosso@gmail.com", "06 BP 2795 ABIDJAN 06", "ABIDJAN/COCODY 2PLTX"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SANTE DE VIE", "TIA LOU BOTI M.CHANTAL", "phciesantedevie@aviso.ci", "27 BP 555 ABIDJAN 27", "ABIDJAN II PLTX"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FADYL", "TOE OUMAR AIME", "pharmaciefadyl@gmail.com", "10 BP 1982 ABIDJAN 10", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EUREKA", "TOKPA LAURENT", "laurenttokpa@yahoo.fr", "BP 221 ADZOPE", "ADZOPE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAFIR", "TOLO AWA", "toloj22@yahoo.fr", "22 BP 244 ABIDJAN 22", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU FUTUR", "TOURE YSSOUF", "pharmaciedufutur2016@gmail.com", "BP 16 ABIDJAN CIDEX 03", "RIVIERA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES 4 ETAGES", "TRAORE BONI DESIREE MICHE", "phcie4etages@hotmail.fr", "06 BP 1250 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ANALYA", "YANGNI N'DA YVES STEPHANE", "PHCIEanalya@gmail.com", "18 BP 187 ABIDJAN 18", "COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST FRANCOIS XAVIER", "YAPI HONORE", "yapih64@yahoo.fr", "04 BP 1571 ABIDJAN 04", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT-ANGE", "YAPI SOPIE MATHILDE", "psa_attoban@yahoo.fr", "01 BP 7032 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EMMANUEL", "GBOGBOUO TRA GERMAINE", "georgetteg2000@yahoo.fr", "", ""));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE D'ABOBO-BAOULE", "YOBOUE HEO SERGE", "serge_yoboue@yahoo.com", "08 BP 952 ABIDJAN 08", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LAURIERS ATTOBAN", "ABONG JEAN-JACQUES", "ohandzabong@yahoo.fr", "11 BP 334 ABIDJAN 11", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SERVIR SARL", "AHO JOEL RICHARD SEKA", "nouvellePHCIEtsacoe@yahoo.fr", "14 BP 345 ABIDJAN 14", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BETHEL", "AKA ESSOI JACQUES", "aboutou2008@yahoo.fr", "COCODY-RIVIERA ATTOBAN", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LA REDEMPTION SARLU", "AKE N'GUESSAN VICTOR", "victorakenguessan@yahoo.fr", "06 BP 1200 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EXPERANCE COCODY", "BAYOH KALIDJA", "bayohkad@yahoo.fr", "BP 563 CIDEX 3 ABIDJAN", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVIN AMOUR", "BEDA YAPI LAMBERT GUY", "phciedivinamour@yahoo.fr", "06 BP 1983 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES GENERATIONS", "BONI INNOCENT GEORGES ALEX", "alex.boni@ordrepharmacien.ci", "11 BP 2918 ABIDJA 11", "MONTEZO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("LA GRANDE PHCIE DU BIEN ETRE", "CODO EPSE KEITA ELIETE", "lagrandePHCIEdubienetre@gmail.com", "06 BP 1502 ABIDJAN 06", "ABIDJAN/COCODY 2 PLTX"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE APPAUL ABJ.", "COFFIE GOUDOU RAYMONDE", "pomeliecoffie@yahoo.fr", "25 BP 628 ABIDJAN 25", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MERMOZ", "COULIBALY MOMO MADELEINE", "c.momo@live.fr", "08 BP 2667 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TEHOUA", "COULIBALY MOUNIRATOU CHAR", "mounirat14@gmail.com", "06 BP 1172 ABIDJAN 06", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA 7ÈME TRANCHE", "DAYATO AZAN COMOE P", "phcie7etranche@aviso.ci", "CEDEX 03 BP 343 ABJ RIVIERA", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU BONHEUR SARL-U", "DIBY JOSEPH", "jdibyk@yahoo.fr", "25 BP 112 ABIDJAN 25", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES ARCADES", "DIOMANDE TOUMOUTOUKA", "PHCIElesarcades@yahoo.fr", "28 BP 1037 ABIDJAN 28", "ABIDJAN II PLTX"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME GRACES/COPHAR", "DJAHA KOUASSI CLAUDE", "djahaclaude@yahoo.fr", "13 BP 2801 ABIDJAN 13", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR DIALLO", "DJIRO GAFARANGA CLEMENTIN", "djiroclemy@yahoo.fr", "17 BP 124 ABIDJAN 17", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EPHRATA", "DOUMATEY SOPPI SYLVIE", "PHCIEphrata@gmail.com", "18 BP 2071 ABIDJAN 18", "ABIDJAN COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES 7 LYS", "FADIGA SOUMAH YAKHA", "phcie7lys@yahoo.fr", "BP", "ABIDJAN/ANGRE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("SUD PHARMA", "GARBA MOUSSA", "", "25 BP 154 ABIDJAN 25", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIEV LA VIERGE DU SIGNE", "GBANGBO EPHILIET KOKOKPLY", "blackwhitehouse@yahoo.fr", "25 BP 702 ABIDJAN 25", "ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LA MISERICORDE", "GOHEAN DOHO BERTIN", "gohean@outlook.com", "BP", "ABIDJAN/ANYAMA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ENICA", "GORE BI TRA JEAN-SYLVAIN", "gjeansylvain@hotmail.com", "08 BP 4216 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("HOPITAL PSYCHIATRIQUE", "HOPITAL PSYCHIATRIQUE", "", "05 BP 84 BINGERVILLE", "ABIDJAN/BINGERVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("ADEC PHARMA", "KABRAN YAO JEAN ALBERT", "", "11 BP 869 ABIDJAN 11", "ABIDJAN/COCODY II PLATEAUX"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LA PAIX", "KADJO THOKOUT EVRARD RAOU", "evrard_thokout@yahoo.fr", "13 BP 3146 ABIDJAN 13", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE MONIQUE", "KOFFI KRA", "", "21 BP 187 ABIDJAN", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES PERLES", "KONAN ANNETTE", "lesperles.kannette@hotmail.fr", "01 BP 1512 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR ANCIENNE GEND", "KONAN FIRMIN DANIEL", "danielkonan@yahoo.fr", "BP 855 ANYAMA", "ANYAMA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MARCHE AKEKOI", "KONATE ARAMATOU", "matoukci05@yahoo.fr", "BP", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA CITE SIR", "KONIN BOUAFFON PIERRE", "bouaffonpierrekonin@yahoo.fr", "25 BP 2350 ABIDJAN 25", "ABIDJAN COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CELLULE SAINE", "KOUADIO ODI DIDIER", "odikouadio@gmail.com", "01 BP 1975 ABIDJAN 01", "ABIDJAN/COCODY 8EM TRANCHE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME DU REFUGE", "KOUADJO NEE ANGORAN ARMEL", "phcienddurefuge@gmail.com", "22 BP 697 ABIDJAN 22", "ABIDJAN/COCODY RTE BGVIL"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MILIE HEVIE", "KOUAO AKA AUGUSTIN", "pharma_miliehevie@yahoo.fr", "13 BP 1001 ABIDJAN 13", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BELLE EPINE", "LEPREON FLORA-COLOMBE", "", "22 BP 1336 ABIDJAN 22", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE 8EME TRANCHE", "LEPRY TCHEMELE LINDA", "phcie8tranche@yahoo.fr", "06 BP 6668 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES ROSIERS", "N'GUESSAN YOLANDE", "yolandenzi@yahoo.fr", "25 BP 1674 ABIDJAN 25", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARIE ESTHER", "N'GUETTA KOUAO ALAIN CLAU", "PHCIEmariesther@gmail.com", "11 BP 969 ABIDJAN 11", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AN-NASR", "OUATTARA ADIDIATA", "adiouatt@gmail.com", "BP 84 ANYAMA", "ANYAMA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LAURIER 9", "OURAGA CASIMIR", "", "01 BP 12431 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ROUTE AKEKOI", "SAI DONH GBATO", "saidonhgbato@yahoo.fr", "14 BP 1494 ABIDJAN 14", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES 7 COLONNES", "SANLE ELLO YVONNE", "les7colonnes@gmail.com", "01 BP 7421 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES CATLEYAS", "SIMAGA EPSE SERY FATOUMAT", "ftsery@yahoo.fr", "20 BP 1445 ABIDJAN 20", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PALM CLUB", "TANOE DOMINIQUE FABIENNE", "dominiqueroth@aviso.ci", "BP 394 CIDEX 01 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("LA GRANDE PHCIE DE GBAGBA", "TANOH DJINDOU TIMOTHE", "ttanoh@yahoo.fr", "BP 474 BINGERVILLE", "BINGERVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE COMOE", "WOLBER EDWIDGE", "wolberpharcie@yahoo.fr", "08 BP 624 ABIDJAN 08", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("CENTRAL PHARMA PST", "CENTRAL PHARMA PST", "pharmastetrinite@outlook.fr", "06 BP 163 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("CONSEIL NATIONAL ORDRE PHCIEN", "CONSEIL NAT. ORDRE PHCIEN", "", "06 BP 942 ABIDJAN 06", "COCODY/2 PLATEAUX"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA PAIX", "DOUZOUA LUCIEN", "pharmaciedelapaix@live.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LE TREFLE", "LASSEY DANIELLE", "danylassey1@yahoo.fr", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU SOLEIL", "SIBAILLY ARSENE", "PHCIEdusoleil@aviso.ci", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA BIA", "DINDJI JULES", "dindjijules@aviso.ci", "", "ABOISSO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE THERESE SARL", "AYEPA GEORGES", "ayepa@hotmail.com", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA GALERIE SARL", "KACOU AIME ARSENE", "kacouedoua@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES ENTREPRISES", "ASSAMOI ALFRED", "alfred.assamoi@yahoo.fr", "", "ABIDJAN/VRIDI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE POLYCLINIQUE FARAH", "KONAN AHOUI APPOLINAIRE", "valahoui@gmail.com", "", "MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE KOUMASSI", "KEITA JEAN MAMOUDOU", "PHCIEkoumassi@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE BIETRY SARL P", "ADOUMEL AGNERO THOMAS", "pharmabietry@gmail.com", "", "ABIDJAN/BIETRY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AKWABA", "YACE DOMINIQUE", "Phcieakwaba02@yahoo.fr", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ANIAMAN", "DOSSO LOSSENI", "PHCIEaniaman@hotmail.fr", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CAMPEMENT", "PAPALY EPSE MAMBEY ADJOA", "papaly_sandra@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA ZONE III SARL.U", "KEITA AUBRY PASCALE", "PHCIEzone3@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MARAIS", "KOUASSI MICHEL SERAPHIN", "PHCIEdumarais@live.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PERUSIA", "DIBI IRENE", "drdirene@yahoo.fr", "", "ABIDJAN/ZONE 4"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU GRD MARCHE SARL BONOU", "YAPO AHUE SOPIE DANIELLE", "phciegdmarche@yahoo.fr", "", "BONOUA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'OCEAN", "AKA AOUELE", "aouele@afnet.net", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU PALAIS DE JUSTICE", "DIADHIOU ADAM AGATHE", "atsepodiadhiou@yahoo.fr", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FANNY", "TOURE MAMA FANNY", "phcie_fanny@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ES'BIA", "SOBRO OUTOU SALOMON ( Rachat)", "phcie.lilymarie@gmail.com", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KAHIRA", "TRAORE DJENEBA EP. KEITA", "phciekahira@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MALON", "KOUROUMA IDRISSA", "phciemalon@yahoo.fr", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MARCHE", "DIALLO OUSMANE", "phciediallo@aviso.ci", "", "ABOISSO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EBATHE SARL", "ABLE DELPHINE", "konan_ferdi@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BABAKAN SARLU", "KOFFI BERNADIN", "haignonkb@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU GRAND MARCHE GRD-BASS", "KOMENAN AROCHY BERTHILDE", "phciedumarchegb@gmail.com", "", "GRAND-BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BETHY DE N'DOUCI", "N'GUESSAN BETHY HABIB MIC", "mbethy@hotmail.com", "", "N'DOUCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PERGOLA (RACHAT)", "SISSOKO FATOUMATA JEANNE", "fatsiss2000@yahoo.", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'INJS", "ABONG EHOUNOUD A THERESE", "phinjs@aviso.ci", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BENABIE", "ACKAH BENIABIE", "ackah597@gmail.com", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'ENTENTE", "ARKURST M. BERNADETTE", "okabernadette@hotmail.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE VRIDI SIR", "DEIGNA NICOLE", "phcievridisir@yahoo.fr", "", "ABIDJAN/VRIDI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES ARCHANGES", "SANLE JEAN CRESORT", "phcie_arch@aviso.ci", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PH DE WILLIAMSVILLE SARL", "KEHIRI LYDIE", "lydie-kehiri@live.fr", "", "ABIDJAN/ADJAME-WILLIAMSV"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE UNIVERS", "SIRIBIE MOUGNINA ALIMATA", "pharmaci.univers@gmail.com", "", "KADIOGO/BURKINA FASO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE IMMACULEE", "KOFFI ALAIN", "alainkoffi7@gmail.com", "", "MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TSF-MARCORY-SARL", "AHOUSSOU YAO VINCENT", "PHCIEtsf@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE PORT BOUET SARLU", "GBAKA TCHETCHE NANA", "nanaboukary@yahoo.fr", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'OREE", "WOGNIN ELAUD NOEL", "PHCIEdeloree@yahoo.fr", "", "BONOUA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE BONOUA", "TANOH FATOUMATA", "phciebonoua82@gmail.com", "", "BONOUA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE ADELE SARL", "ABOUYA EPSE YAYO MIREILLE", "PHCIEsteadele@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE FOI", "SOUMAHORO AMARA", "pharmastefoi@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE MARIE MAJEURE", "AKA N'GUESSAN LYNE WAYOU", "lyneaka@hotmail.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE D'ADZOPE", "N'GUESSAN KONAN BERTIN", "tinbernguessan@yahoo.fr", "", "ADZOPE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AVENUE CHARDY SARL.U", "LY DJENEBA", "pharmavenuechardy@gmail.com", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA PLACE MARCORY", "N'DIAYE ANNA EPSE COULIBA", "partner18fr@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES REMBLAIS", "ANOUMA DINGUI MADELEINE", "anouma.madeleine@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BELLEVILLE", "BAH DIENEBA", "Phciebellevilletreich1@aviso.ci", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU PETIT MARCHE", "BITTY MARIE NOELLE", "", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU GRAND MARCHE", "DIPLO WANDA", "pgmtreich@gmail.com", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA BALTIQUE", "LOUKOU N'GUESSAN ROSINE", "PHCIEbaltique@gmail.com", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES PLAGES SARL U", "NIAMKEY MIEZAN A FLAVIEN", "PHCIEdesplages@gmail.com", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE & LABO.PLACE DE LA REPUB", "SANGARE ASSANA", "phcielaboplr@yahoo.fr", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE L'AVENUE 21", "TANO ASSA", "assatano@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRANDE PHCIE DE L'AGNEBY", "SEUMEUN ATHANASE GONLEGBE", "pharmagneby@yahoo.fr", "", "AGBOVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRANDE PHCIE DES BANQUES SARL", "KONE AMINATA", "konaninat@yahoo.fr", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE LES HARMONIES", "CAMARA MARIAM ESTHER", "cmariamesther@yahoo.fr", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA BAGOE SARL", "DIAWARA KONE FATY", "labagoe@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE VRIDI VILLAGE", "KOFFI EMMANUEL", "dremmanuelkoffi@gmail.com", "", "ABIDJAN/VRIDI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ROUTE DE BASSAM", "YEO ALI", "prb2004@outlook.fr", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ELOMA DE GONZAG", "AKANI ADUENI", "pheloma@hotmail.co", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE INCH-ALLAH SARL", "AGNIMEL MARINA BENEDICTE", "phcieinchallah@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME D'AFRIQUE", "AMARI NICAISE", "phcie.nda@gmail.com", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRDE PH MAHANDIANA DE MARCORY", "FOFANA KALILOU", "fkalilou@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CENTRALE DU PLATEAU", "FARHAT EPSE EL AMINE L.", "malackajami@hotmail.fr", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE VRIDI PALM BEACH SARLU", "TUAN YAH KANGA LILIANE", "phciev.palmbeach@yahoo.fr", "", "ABIDJAN/VRIDI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LE ROC", "GBOHO JUSTIN", "docteurkouakouoswald@gmail.com", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLLE PHCIE DES QUAIS SARL", "BOSSE EPSE KEHIN ONEA BEL", "dominiquebossekehin@gmail.com", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHATELET", "NATCHIA ETTE WOLFGANG", "natchiaette@live.fr", "", "PORT BOUET/GONZAGUEVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT ANTOINE D'ABOBO", "NIANGORAN MELANIE", "niangoranmelanie@yahoo.fr", "", "ABIDJAN/ABOBO GARE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU PETIT MARCHE", "OHOULAN-OKA OUETCHIN VICKY NIN", "evickynina@yahoo.fr", "", "GRAND BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GRACE ORILA", "DJOHOU PHILIPPE", "djohou.phillipe@gmail.com", "", "RUBINO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE BROFODOUME", "N'DJA BROGOLI SERGE DIDIER", "ndjaserge28@gmail.com", "", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE YAOU SARL.U", "AKOMIEN ANNICK MARIE", "akomien@hotmail.com", "", "GRAND-BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRANDE PHCIE D'AKOUPE SARL.U", "OBOU MENE DEMOSTHENE", "d.obou@yahoo.fr", "", "AKOUPE ROND POINT ROUTE AFFERY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT JEAN-PAUL II", "TCHAOU ABO ODILE", "odile.abo@gmail.com", "", "RIVIERA BONOUMIN"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE J.TIACOH (SARL.U)", "TIACOH JULIE", "drjulietiacoh@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CARREFOUR MARCORY", "ASSAHI GRAH GREGOIRE", "phcicarrefour@aviso.ci", "", "ABIDJAN/MARCORY"));

        // Liste supplémentaire de pharmacies
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHICAYA", "CHICAYA EVELYNE", "echicaya@yahoo.com", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TERMINUS", "DAIPO GUY", "guydaipo@yahoo.fr", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PIERRE&MARIE CURIE", "DEM BRAHIMA", "PHCIEpm.curie@aviso.ci", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT FRANCOIS", "ANNA MARIA DIOP", "mariadre@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BOULEVARD CARDE", "BROU KOFFI RENE", "brbrouko@yahoo.fr", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MEROUANE", "FOFANA COULIBALY AMINATA", "amifofc@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU LEVANT", "GUIBONY MOCKEY IRENE", "phcilevant@gmail.com", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT GEORGES", "LOKROU BOHUI MARTIAL", "mblokrou@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU PHARE (RACHAT)", "ANGOFI MARIE ANGE", "nvelpharmacieduphare@gmail.com", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NIACADIE", "NIACADIE VICTOR", "phcieniacadie@yahoo.fr", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'AMITIE", "NUAN-ALIMAN ELISABETH", "PHCIEdelamitié@gmail.com", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES TROIS MOSQUEES", "SYLLA AICHATOU", "ph3mosquees@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'ARRAS SARL", "TCHIMOU KOUTOUAN & ANNEY", "madoack@gmail.com", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FRANCE AFRIQUE", "TOURE FADIGA", "off.franceafrique@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DJIGUI", "TRAORE ABDOURAHAMANE", "traoreabdourahamane@yahoo.fr", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CLIMBIE", "WALEME ELISABETH EP.OBBIN", "climbie55@hotmail.com", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE WASSIA", "ZEBE-OBRE GILBERTE", "pharmacie.wassia@gmail.com", "", "GRAND-BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE NANAN YAMOUSSO", "DIABY DAOUDA", "diabydaoudad23@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SANTE POUR TOUS", "BRUNO BASILE", "dr_basilebruno@yahoo.fr", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MOAYE N'ZIANOUAN", "DJETTO LAGO MAXIME", "djettom2008@yahoo.fr", "", "DALOA/N'ZIANOUAN"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ODE", "AHO AKANDAN TCHIMON MICHE", "ahomichel31@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU SANWI", "AKA BILE JOSEPH", "niamkeanniep@yahoo.fr", "", "ABOISSO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CHATEAU AGBOVIL.SARL", "DJAMA ALBERT", "drdjama@aviso.ci", "", "AGBOVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE YELE SARL", "KONE IBRAHIME", "ibrakopharma@yahoo.fr", "", "GRAND-BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT ANTOINE", "N'GUESSAN GUILLAUME", "psazone4@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT SAUVEUR", "BALLET PIERRE MARIE MARC", "phciestsauveur@hotmail.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SINI KENEYA", "ALLOUE SIMON PIERRE", "sinikeneya@aviso.ci", "", "AGBOVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE BIANOUAN", "BROU KOMENAN MARTIN", "martinkbrou@yahoo.fr", "", "BIANOUAN"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE BVD DE LA PAIX", "N'DA PATRICE DESIRE INNOC", "patricen2011@hotmail.com", "", "ABIDJAN/ATTECOUBE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE D'AYAME", "ZOUMANA SANGARE", "sangarezoumana2@gmail.com", "", "AYAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE JEANNE", "MANGOUA REGINA ESTHER", "PHCIE.stejane@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT LUCIEN", "IBO CATHERINE EPSE ODI", "pharmastlucien@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRANDE PHCIE IROKO", "ALLAH-KOUADIO ANDRE", "gphiroko@aviso.ci", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LEILA", "KONE MAKAN", "phcieleila@aviso.ci", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT ALBERT", "ASSAVO N'DA N'ZIAN", "assav@aviso.ci", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT LOUIS DE GONZAGUEV", "KODJA NEE EBOUA GNIMA M.T", "mariekodja@yahoo.fr", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA VIE", "ANAGONOU SONIA NEE FOLY", "affouengotta@gmail.com", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DOLIELMA", "DEMPAH ANOH JOSEPH", "josephdempah@yahoo.com", "", "ABOISSO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ANIEL", "COFFIE CADIO JUSTIN COME", "PHCIE_aniel@yahoo.fr", "", "AIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERNE ADIAKE", "SANOGO YAYA", "sanogoyaya@yahoo.fr", "", "ADIAKE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SOKOURA D'ABOISSO", "VAMI BI NENE JOSEPH", "vamibij@yahoo.fr", "", "ABOISSO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE QUARTIER DIVO", "GBANE ALIADJI DAOUDA", "PHCIEquartierdivo@gmail.com", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE SOPIM KOUMASSI", "N'GOSSAN EHOUMAN DAVID", "PHCIEsopimkssi@yahoo.fr", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA CATHEDRALE SARL U", "KOUADIO DJAHA ALAIN", "phciedelacathedrale@gmail.com", "", "GD BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DJESSOU", "COULIBALY NOUMORY", "counoum@yahoo.fr", "", "ABIDJAN PORT-BOUET VRIDI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE DE N'DOUCI", "ATTEMENE DADIE FULGENCE D", "attemenefulgence@yahoo.fr", "", "N'DOUCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BD DU CAMEROUN", "DAGOU ADON CHRISTOPHE", "dagouchristophe@gmail.com", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MISERICORDE", "ADIEKA ERNERST", "adieke.elite@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE INOX SARLU", "KAKOU GUY MARCEL", "inoxpharma_ci@yahoo.fr", "", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE HELENE", "AKA DIDIER STEPHANE EULOG", "yapadepetar@gmail.com", "", "MAFERE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE JEHOVA-JIRE SARL.U", "ADOUENI EPSE KADJI ALBERT", "kadji1980@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MARCHE SIKENSI", "ANO KOFFI JEAN", "pharmaciedumarchesikensi@gmail.com", "", "DABOU-TIASSALE-GD LAHOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA VIE (RACHAT)", "N'GOTTA AFFOUE", "affouengotta@gmail.com", "", "MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CRHIST'EMILE", "KOUASSI ERIKA EPSE AKA", "erickaclaude@yahoo.fr", "", "KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME DES VIVTOIRE", "KOUDOUGNON NOEL CYRILLE", "ckoudougnon@gmail.com", "", "ABOISSO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE CLEMENTINE", "KOUAME ASSACY K.NATHALIE", "assacnath@gmail.com", "", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SANTE POUR TOUS D'ANYAMA", "GBANGBO EPHILIET YVES MAXIME", "gbangboyves@yahoo.fr", "", "ABIDJAN/ANYAMA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES SABLES", "BROWN EDDY MANSAN", "phsables@aviso.ci", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE REGINA", "COULIBALY NEE DODO REGINE", "ndjechia@gmail.com", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE RUE 12", "BENON ALIMATA", "pharmacie_rue12@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE BVD GISCAR D'ES", "KOUADIO ANNE FAUSTIN", "doc.anne@hotmail.com", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("CATALYSE BEAUTE-SANTE", "LASME DAGO CHRISTELLE", "chdago@ymail.com", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE ELITE (RACHAT)", "CISSE M'BALET MARHYAME", "", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KOFFI KRA", "KOFFI KRA AIME", "pharmaciekoffikra1@gmail.com", "", "ABIDJAN/PORT-BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOYA", "N'GUESSAN FABRICE EDITH", "nfabriceedith@ymail.com", "", "YOPOUGON EST"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT-LOUIS", "VABRE JUSTINE", "stlouis@aviso.ci", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FLORA", "ANOH ROSE ARMANDE", "anohrose@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE M'PIKE", "KOUASSI NOGBOU CATHERINE", "pharmaciempike@gmail.com", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KAEMILLA SARL U", "BALLO SARAH MEITA ABIBATA", "sarah.ballo25@gmail.com", "", "ABIDJAN/BIETRY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT PAUL D'AKROMIABLA", "TRAH BI LALA PAUL", "phciestpaul@gmail.com", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE TRINITE ABOISSO", "ADJI KOFFI SERGE DESIRE", "sergeadji7@yahoo.fr", "", "ABOISSO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BETHANIE", "EBROTTIE ANEY FREDERIC", "phciebethanie@aviso.ci", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE JEANNE-D'ARC", "TOURE NOELLE EPSE TAYORO", "phciestejdarc.c@gmail.com", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ASET LA DIVINE", "N'GUESSAN AMOUA LYDIE", "pharmaset@gmail.com", "", "AZAGUIE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MOCKEYVILLE", "NAYEHOUN KOUASSI SERAPHIN", "nayehoun2008@yahoo.fr", "", "GD BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVINE MISERICORDE", "ANDJI-BLA YOLANDE OLGA", "yolandeolga@yahoo.fr", "", "ABOISSO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BIABOU MOSQUEE", "KOUAME EPSE AHOUTI DANIELLE", "ahoutide@yahoo.fr", "", "ABIDJAN/ABOBO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVIN-AMOUR DE N'ZIKRO", "ESSI HOUA DAVID OLIVIER", "essihoua@yahoo.fr", "", "BONOUA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PH.CENTRAL PHARMA PDP", "CENTRALE PHARMA PDP", "pharmacieduport@aviso.ci", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ALPHA 16", "CISSE MASSOGBE", "cissemassogbe@yahoo.fr", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MESANO", "MESSOU ANOI", "", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MERIDIEN/COPHARMCI", "ABE SYLVESTRE", "phciemeridien@gmail.com", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE APROMEX PHARMA SARL", "ASSOUMAN KOUAME LOUKOU R.", "", "", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE CANAAN SARL", "BLOHOUA ERICK OLIVIER", "b_erickci@yahoo.fr", "", "ABIDJAN/ZONE 4 RUE PAUL LANGEVIN"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CANAL", "OULAI SEUNDE URBAIN", "phieducanal@aviso.ci", "", "ABIDJAN/KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("S.A.D.P. PARASHOP-FLOR'", "S.A.D.P. PARASHOP-FLOR'", "", "", "ABIDJAN/TREICHVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LANVIA SARL", "DIOMANDE MOUSSA SINZE", "dagouchristophe@gmail.com", "", "MARCORY ZONE 4"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE QUARTIER FRANCE", "ACHI JUSTE APIE", "justeachi@ymail.com", "", "GRAND BASSAM"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MASSARANA (COURANT)", "KASSI ARMAND GEORGES", "armandkassigeorges@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BETHEL", "GNADOU LOUBA FAUSTIN", "gnadoufaustin54@gmail.com", "", "PORT BOUET"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA RIVE GAUCHE", "LOUKOU DONANTCHAT DURAND", "aristideloukou@yahoo.fr", "", "ABOISSO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SHALOM DE NOE", "EFFIBI GNAKON CHANTAL", "keffeb@yahoo.fr", "", "ABOISSO/NOE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU PONT D'ANOUMABO", "KPLE AHOURE LAMI ALEXANDR", "phciedupontanoumabo@yahoo.com", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE FRATERNITE", "BOSSE BALLOU DIEUDONNE K.", "kevinbossebd@gmail.com", "", "ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EL-OLAM D'ANOUMABO", "DOMO ABOUDE JEAN-CLAUDE", "daboudejeanclaude@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("SRS MISS.STE THERESE ENFT JES", "SRS.MISS.STE THERESE E.J.", "", "", "KOUMASSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ROMA D'ANOUMABO", "CISSE SAVANE MASSANDJE", "sav_mass@yahoo.fr", "", "ABIDJAN/MARCORY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PPM INTERNATIONAL", "PPM INTERNATIONAL", "c.diallo@ppminternational.fr", "", "ABIDJAN/COCODY DANGA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AKADJOBA", "AGOUSSI MARIE-GERMAIN", "dr_agoussi@yahoo.fr", "04 BP 1747 ABIDJAN 04", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVINE ESPERANCE SARL U", "APATA PELAGIE EPSE BOGLO", "pelagieb@yahoo.fr", "14 BP 1067 ABIDJAN 14", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GENERAL DABOU", "BAMBA KARAMOKO", "grandePHCIEdedabou@gmail.com", "BP", "DABOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SANDRINA", "BECHIE N'GUESSAN", "bechieyapo@yahh.fr", "01 BP 3769 ABIDJAN 01", "YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE MARIE/TIASSALE", "BELLA DIDIER", "bdidier555@yahoo.fr", "BP 555 TIASSALE", "TIASSALE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE KETTE", "DACOURY TABLEY PIERRE", "pierredacourytabley@yahoo.fr", "BP 161 AKOUPE", "AKOUPE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PORT-BOUET II SARL", "EDOUA KACOU JACQUELINE", "mamyjacquie@yahoo.fr", "06 BP 880 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU LAVAGE SARL", "EHOUNOU KADJO NAZER", "ehounoukn@yahoo.fr", "23 BP 3045 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ANANERAIE SUARL", "GALLE ATTABLI ANSELME", "galleatabli22@yahoo.fr", "BP YOPOUGON", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU PROGRES", "KACOU BEUGRE ELISABETH", "eblkacou@hotmail.com", "02 BP 750 ABIDJAN 02", "ABIDJAN/WILLIAMSVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES GARES SARL", "KIRE KOUADIO NOEL", "kirenoel@yahoo.fr", "02 BP 153 ABIDJAN 02", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR KOWEITT", "KOFFI ETTE GEORGES", "ettegeorges@yahoo.fr", "30 BP 358 ABIDJAN 30", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EDEN", "KOFFI KOUASSI HERVE", "", "22 BP 170 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU BANCO SARL", "KONE DRAMANE", "fanthakone@yahoo.fr", "03 BP 654 ABIDJAN 03", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT ANDRE SARL U", "KONE LANDOLO ALPHONSE", "landelor1@yahoo.fr", "21 BP 825 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE JEAN-PIERRE", "KOUA TANOH ROGER", "PHCIE.jeanpierre@yahoo.fr", "BP", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT LAURENT", "KOUASSI ATSE CEDRIC", "", "21 BP 2363 ABIDJAN 21", "ABIDJAN/YOPOUGON"));

        // Liste supplémentaire de pharmacies
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AIMESS", "KOUASSI ESSO JEANNETTE F.", "", "20 BP 582 ABIDJAN 20", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE TOIT ROUGE SUARL", "LASME DIOMANDE HERMANCE", "hermlas@yahoo.fr", "25 BP 1084 ABIDJAN 25", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LE BELIER", "LY SOMALI AWA", "PHCIE.belier@yahoo.fr", "01 BP 4810 ABIDJAN 01", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MAI", "MAI CLAUDE & KOUASSI EUGE", "", "BP 76 AGNIBILEKROU", "AGNIBILEKROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CHATEAU D'EAU", "M'BAHIA NEE GUEYE LIKA", "", "01 BP 3673 ABIDJAN 01", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MAKISSI", "NAHO YÉZOU LAURENT", "", "01 BP 1464 ABIDJAN 01", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES LAGUNES SARL U", "N'DIAYE ZOUBAYROU", "", "BP 323 DABOU", "DABOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT EMMANUEL", "NOGBOU DIDIER", "maguinobou@yahoo.fr", "06 BP 2818 ABIDJAN 06", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BONDOUKOU-MARCHE", "OUATTARA AUGUSTE SOULEYMA", "bel.fam.ao@gmail.com", "BP 25 BONDOUKOU", "BONDOUKOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT LAURENT-ADZOPE", "SAHLY PATRICIA N'DELI", "phciesaintlaurent2004@gmail.com", "BP 241 ADZOPE", "ADZOPE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MARCHE ABENG.", "SENIN DON MELLO P.", "phciemarche@aviso.ci", "BP 168 ABENGOUROU", "ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT MICHEL", "SIBAILLY ASSAMOI LOUISE", "stmichelpharma@gmail.com", "03 BP 425 ABIDJAN 03", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRANDE PHCIE DE DABOU SARL.U", "SOMBO JEAN-BAPTISTE KEVIN", "ksombo@laborexci.com", "BP 237 DABOU", "DABOU-TIASSALE-GD LAHOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT CLEMENT SARL", "SONHON ANET JUSTINE", "", "06 BP 1278 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DESBRONS", "SOROHO GBARAMA", "sorohogbarama@hotmail.com", "BP 97 TANDA", "TANDA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SIPOREX", "TANO ADJO MARIE", "PHCIEsiporex@hotmail.fr", "21 BP 814 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ZONE INDUSTRIELLE YOP", "TOURE KOUAME CONSTANCE", "", "22 BP 1366 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE AWALE (RACHAT)", "WANDJI ZAROU BORIS CLOVIS", "wandjiboris@yahoo.fr", "23 BP 2992 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST ETIENNE SARL", "YAPO FRANCK DESIRE ACHIEP", "PHCIEsaintetienne@yahoo.fr", "23 BP 2318 ABIDJAN 23", "YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE CHOQUET", "ZOGUEI DAHAHON B.OLGA CHR", "olgazog2004@yahoo.fr", "BP 63 BONDOUKOU", "BONDOUKOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SELMER", "ACAFOU AIME-PLACIDE", "acafouaime@yahoo.fr", "03 BP 3136 ABIDJAN 03", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR BOBY SARL U", "ADHOUT CLAVER ALAIN", "cadhout3@hotmail.fr", "21 BP 4395 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SANTE PLUS", "ADJA BOSSOMA MARGUERITTE", "psanteplus12@yahoo.fr", "06 BP 1409 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PROVIDENCE /ABENGOUROU", "ADJIMI LEDJOU JEAN LUC", "adjimijl@yahoo.fr", "BP 574 ABENGOUROU", "ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT URIEL PAILLET SARL", "AFFRO NEE AKPA JOCELYNE", "akpajoce@gmail.com", "22 BP 404 ABIDJAN", "ABIDJAN/WILLIAMSVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUVELLE SARL U", "AGOH KOUASSI ROLAND", "rol.agoh@yahoo.fr", "BP 84 BONDOUKOU", "BONDOUKOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BETHEL DE BEAGO", "AGUIDOU N'GUESSAN HERVE", "dr_aguidouherve@yahoo.fr", "23 BP 3421 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AMIRA SARL", "AHMED TOURE", "atoure31@hotmail.fr", "BP 327 ADZOPE", "ADZOPE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ESTHER", "AKELY AKA", "akelyaka@yahoo.fr", "23 BP 3934 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MARCHE YOPOUGON", "AKO AKE ACKA CONSTANT", "akec18@yahoo.com", "22 BP 292 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE D'ABOBO DOUME", "AKRE BEUGRE THEODORE", "theodoreakre07@gmail.com", "01 BP 1742 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AGOUA SARL.U", "AKRE MOBIO CHRISTOPHE", "akrechristophe@yahoo.fr", "19 BP 396 ABIDJAN 19", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EBILASSO", "AMEKOU N'DRI FIRMIN", "amekoufirmin@yahoo.fr", "BP 1181 ABENGOUROU", "ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GNIMAH", "ASSOUMOU N'ZI DANIELLE", "assoumoudanielle@yahoo.fr", "22 BP 1042 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHRIST ROI", "ASSOUNDJARY TAH KOBENAN H", "", "04 BP 2623 ABIDJAN 04", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ROXANNE", "ATTIE JEAN PHILIPPE AMON", "phcieroxanne@aviso.ci", "31 BP 260 ABIDJAN 31", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE RITA", "AYEMOU GOBOH S. CHRISANET", "", "23 BP 3774 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TEREZA", "BARRY OUMOU", "barryoumou@gmail.com", "BP 62 CIDEX 01 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA PROVIDENCE ADJAME", "BEDI MEL SAMUEL", "", "09 BP 1491 ABIDJAN 09", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES ESPERANCES YOPOUGON", "BINDE N'DRAMAN A.AUGUSTIN", "bindendraman@gmail.com", "04 BP 1313 ABIDJAN 04", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ADJAME-BRACODI", "BLEDOU DOMINIQUE", "begnan2000@yahoo.fr", "BP 7871 ABIDJAN", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CINEMA BOISSY", "BLESSO DESROSIERS A.KETHL", "phcieboissy@aviso.ci", "23 BP 433 ABIDJAN", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA FELICITE", "BOTI MARC ARSENE", "marcarseneboti@yahoo.fr", "06 BP 6657 ABIDJAN 06", "YOPOUGON EST"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVINE GRACE YOPOUGON", "CODO AGBO BLANDINE", "codabdivgra@yahoo.fr", "08 BP 964 ABIDJAN 08", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES TROIS PONTS", "COULIBALY BAKAYOKO AMINAT", "phcie3pt@aviso.ci", "21 BP 1773 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE GESCO SARL.U", "COULIBALY KONATE FANDA", "phciegesco@yahoo.fr", "22 BP 338 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHIGATA SARL", "COULIBALY SALIMATA", "lisayachoumou@yahoo.fr", "22 BP 537 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE HEREMANKONO", "DASSE AGUI TOUSSAINT", "dasseat@yahoo.fr", "BP 68 TIASSALE", "TIASSALE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KORO", "DIARRASSOUBA KARIM", "dkarim11@yahoo.fr", "03 BP 2335 ABIDJAN 03", "ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE STE MARIE D'ADJAME SARLU", "DIGBOHOU OKOBET STEPHANE", "okobetstephane@yahoo.fr", "23 BP 2315 ABIDJAN 23", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LE JOURDAIN SARL-U", "DIOLORI ADE LYLIANE", "dioly@outlook.fr", "31 BP 951 ABIDJAN 31", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SENEVE", "DJE KARAMOKO", "kdrdje@yahoo.fr", "09 BP 1010 ABIDJAN 09", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KANA", "DJEDJI NÉE GISELE ANZOUAN", "", "BP 609 ABIDJAN", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE D'ATTECOUBE", "DOSSO GANOUHO", "samoua.nouho@yahoo.fr", "19 BP 92 ABIDJAN 19", "ABIDJAN/ATTECOUBE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES PHALENES", "DOUE FOFANA HAWA", "hawadoue@yahoo.fr", "21 BP 5056 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ARTEMIA", "EFFI N'DOUA ESTHER", "artemia@aviso.ci", "BP 806 CEDEX 3 ABIDJAN 08", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE THEODORA", "ENOU AHOUSSI WILLIAMS", "dktchereman@hotmail.fr", "21 BP 2875 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES BOIS", "ESSE LINA CHARDEN", "", "01 BP 1953 ABIDJAN 01", "ABOUDE MANDEKE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LOCODJORO SARL", "ESSO SERGE DAVY", "essodavy@yahoo.fr", "23 BP 672 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST.HENRI DE YAKASSE-FEY", "GADJI SERGE-ERIC", "gadji01@yahoo.fr", "BP 1122 ABENGOUROU", "ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LAGOMA", "GBATO WAKELL FRANCIS", "gbatofrancis@yahoo.fr", "25 BP 52 ABIDJAN 25", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT RAPHAEL SARL", "GBONON ROSALIE EPSE ASSI", "", "01 BP 2386 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ABRAHAM DE DABOU", "GOHORE-BI GOUE DENIS", "gohorebi@hotmail.com", "21 BP 156 ABIDJAN 21", "DABOU-TIASSALE-GD LAHOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES 3\"Ô", "HAIDARA BEATRICE", "haidarapsp@gmail.com", "BP 522 GRAND-LAHOU", "GRAND-LAHOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRANDE PHCIE L'AMITIE", "IKPE EPSE SORO-NIENESSINAPARA", "", "06 BP 2791 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("MODERNE DE KOUN-FAO", "KABRAN NIANGORAN", "niangoran19@gmail.com", "BP 05 KOUN-FAO", "KOUN-FAO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'HOREB", "KADIO EPSE TAPE AHOU THER", "", "06 BP 408 ABIDJAN", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AFIA", "KAHOA NEE GANMAVO YEDIA D", "", "16 BP 1682 ABIDJAN 16", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA CITE VERTE", "KANTE ISMAEL", "ismokante@yahoo.fr", "03 BP 329 ABIDJAN 03", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CARREFOUR ABENGOUROU", "KANTE MARIAM", "", "BP 48 ABENGOUROU", "ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BEYNIOUAH", "KHISSY BEYNIOUAH FLORENTI", "", "CASIER BICICI N°23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MARCHE D'ADJAME", "KOBLAN HUBERSON", "", "BP 370 ABIDJAN CIDEX 3", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHENAIN", "KOFFI BRINDOU NEE AMAFOU", "", "23 BP 4008 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MOAYE", "KOFFI GUY-ROGER", "guyroger_koffi@yahoo.fr", "32 BP 121 ABIDJAN 32", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE INDENIE", "KOFFI KONAN JOSEPH", "jsakindo11@yahoo.fr", "BP 901 ABENGOUROU", "ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LAOULO", "KOFFI KOUAME RAPHAEL", "koffi.raphael@yahoo.fr", "23 BP 4519 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARIE THERESE D'ADJAME", "KONAN JULES STEPHANE", "jules.stephane@yahoo.fr", "09 BP 1126 ABIDJAN 09", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRUNELLE-MARIE", "KONAN YAO IGOR FULGENCE", "fugee_ko2000@yahoo.fr", "BP 115 ABENGOUROU", "ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST.GEORGES AKOUEDO 2000", "KONATE NIAGNA", "koneaminatt@yahoo.fr", "06 BP 2606 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LA MAISON BLANCHE", "KONE AMINATA EPSE CAMARA", "doubakone@gmail.com", "03 BP 2055 ABIDJAN 03", "ABIDJAN /YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NIANGON LOKOA", "KONE FANTHA EPSE MAMBO", "", "04 BP 970 ABIDJAN 04", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NVLE DES 220 LGTS SARL", "KONE SAPHIATOU EP.AGNINI", "kone.safiatou07@yahoo.fr", "09 BP 3675 ABIDJAN", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE SION", "KORE LEKADOU SYLVIE NOELL", "PHCIEdesion11@gmail.com", "01 BP 4139 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES GRACES", "KOUABLAN MARGUERITE", "", "09 BP 294 ABIDJAN 09", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LA VIE", "KOUADIO FRANCK ARMAND", "pharmalavie@yahoo.fr", "25 BP 435 ABIDJAN 25", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU COMMERCE ABENGOUROU", "KOUAKOU JEAN-LOUIS", "kouakoulouisfr@yahoo.fr", "BP 1444 ABENGOUROU", "ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NIKIBEL", "KOUAKOU KOUADIO EVARISTE", "evaristekouakou@ymail.com", "05 BP 2247 ABIDJAN 05", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE RABBI", "KOUAMELAN ABOI", "", "06 BP 408 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LE LOTUS", "KOUAO BILE RAMATOULAYE", "lysamagali@yahoo.fr", "BP 941 CIDEX 1 ABIDJAN", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES ECLUSES SARLU", "KOUASSI ATTA RAOUL", "attaraoul@yahoo.fr", "08 BP 2014 ABIDJAN 08", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SANTA CATERINA", "KOUASSI BRUNO ABRAHAM", "kouassibruno25@yahoo.fr", "22 BP 152 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE K.BOILOT", "KOUASSI DIAHOU JEAN CLAUD", "lakouaessp@yahoo.fr \nkdiahou@gmail.com", "BP 738 AGNIBILEKROU", "AGNIBILEKROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ALICIA", "KOUASSI EMMANUELLE AMOUE", "eklokossou@yahoo.fr", "10 BP 825 ABIDJAN 10", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES BEATITUDES", "KOUASSI KANGAH LAURE DOMI", "", "06 BP 2486 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE YOPOUGON ANADOR SARLU", "KOUASSI KOUAME", "kouamekwacy01@gmail.com", "09 BP 1298 ABIDJAN 09", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MOKLA SARLU", "KOUASSI KOUASSI", "drkouassik@gmail.com", "01 BP 6029 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE MARIE - YOP.", "LOGON MARIE ANTOINETTE", "phciesaintemarieyop@yahoo.fr", "01 BP 1535 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU JUBILE", "LOROUGNON PAUL ARNAUD", "lpaularnaud2000@yahoo.fr", "21 BP 1222 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KANNIEN SARL-U", "M'BIMBE ADELINE EPSE DOFFONSOU", "adeline.mbimbe@yahoo.fr", "28 BP 1045 ABIDHJAN 28", "ABIDJAN/COCODY ABATTA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUVEAU QUARTIER", "MENSAH LASSEY JONAS", "lasseymens@yahoo.fr", "23 BP 106 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME DE LA PAIX", "MIAN BENIE J.EPSE ANE", "PHCIE.notredame@yahoo.fr", "06 BP 340 ABIDJAN 06", "ABIDJAN/YOPOUGON RTE DABOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA CITE YOP/COPHARMCI", "N'DJORE ALI HENRI", "", "06 BP 414 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MICHEL ARCHANGE", "N'DOUBA KWESSI EFFOUA ANG", "", "31 BP 1005 ABIDJAN 31", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NIANGON NORD", "N'DRI N'GUESSAN SIMONE", "", "23 BP 2034 ABIDJAN", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FEZAN", "NENIN YOUAN LOU BOTI", "", "BP", "DABOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE D'ATTINGUIE", "N'GBECHE ROSINE", "", "21 BP 2396 ABIDJAN 21", "ATTINGUIE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA MAIRIE DE YOPOUGON", "N'GUESSAN EUPHRASIE ZON", "phrasiata@yahoo.fr", "22 BP 1404 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU ROND POINT ATTECOUBE", "N'GUESSAN JEAN PIERRE", "nguessanjp2004@yahoo.fr", "13 BP 1978 ABIDJAN 13", "ABIDJAN/ATTECOUBE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME DES VICTOIRES", "NIAMKEY MARGUERITE M.AURO", "", "01 BP 5081 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ADJAME LATIN", "N'TAYE SIALA ODILE", "ntayesio@yahoo.fr", "25 BP 809 ABIDJAN 25", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRINCIPALE SIKENSI", "OUATTARA NEE KORE MARIAM", "", "22 BP 346 ABIDJAN 22", "SIKENSI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE D'AGOU", "OUFFOUE JULES O.GBOLO", "", "BP 64 AGOU", "ADZOPE/AGOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NOUVELLE PHCIE DU DJUABLIN", "OULA DINHANONAU SYLVAIN", "oula11@yahoo.fr", "BP 333 AGNIBILEKRO", "AGNIBILEKRO"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SCHIBA", "OURAGA KOUDOU ALFRED", "", "08 BP 3130 ABIDJAN 08", "ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GBEDE", "OZOUKOU ZEGA FREDERIC", "", "06 BP 512 CIDEX ABIDJAN 06", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ELIORA DE SONGON", "SAUGONE EP LATTA CARINE PRISCA", "lcarineprisca@yahoo.fr", "09 BP 4483 ABIDJAN 09", "ABIDJAN/SONGON KASSEMBLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE D'AGBAN", "SERI GNOLEBA FAWZILATOU", "", "06 BP 1279 ABIDJAN 06", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PEFINA SARL", "SILUE YESSELIGUENA", "", "17 BP 516 ABIDJAN 17", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KOUTE SARL", "SISSOKO BIOT VICTORINE", "", "06 BP 2063 ABIDJAN", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MAMY ADJOA", "SOUMAHORO ROCKYA FATOU", "soukyatehe@yahoo.fr", "06 BP 2247 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NIANGON SUD", "TAKI ADJO BEATRICE", "", "BP 745 CIDEX 3 ABIDJAN", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE GABRIEL GARE", "TAKI LOU LYDIE EPSE AMON", "lydietaki@yahoo.fr", "01 BP 13344 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PALOMA", "TAPE LEGROHOUAN", "tapelegrohouan@yahoo.fr", "18 BP 1114 ABIDJAN 18", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SCHEKINA - YOP", "TCHUERE ALEXIS", "", "21 BP 5259 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST ANGE EMMANUEL", "THOBOUET SANDRINE CRITIE", "phciestangeemmanuel@gmail.com", "03 BP 270 ABIDJAN 03", "ABIDJAN/YOPOUGON SOGEFIHA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU MARCHE DABOU SARL-U", "TOURE BABACAR", "dr_toure66@yahoo.fr", "BP 364 DABOU", "DABOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ELKANAH", "TOURE WANGOYO ANGE", "wangoyoa@yahoo.fr", "21 BP 1422 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES ELYSEES", "TRA MORIGBE SILUE PHILOME", "mtsphilo@yahoo.fr", "06 BP 1431 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MOYA/ABENGOUROU", "WASSA AUGUSTE (MOYA)", "auguswassa@yahoo.fr", "BP 1181 ABENGOUROU", "ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES NATIONS", "YAO JEAN CLAUDE", "yaodjevie@gmail.com", "27 BP 116 ABIDJAN 27", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR SONGON DAGBE", "YAZI IKPO JEAN ARNAUD", "jean.YAZI@copharmed.com", "01 BP 11757 ABIDJAN 01", "ABIDJAN/SONGON DAGBE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'ANTENNE", "YEKPE MARIE NICOLE", "PHCIEantenne@aviso.ci", "01 BP 5367 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AZITO", "ZAMAN ZABLE JUSTINE", "zablejustine@yahoo.fr", "07 BP 249 ABIDJAN 07", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TIZRA", "TIE BI ZAMBLE DENIS", "", "23 BP 3564 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE L'EMMANUEL", "ADOU ASSOKOU ALEXIS", "", "06 BP 6161 ABIDJAN 06", "ABIDJAN/WILLIAMSVILLE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT HERMANN", "AGBASSI N'SOUA HIPPOLYTE", "st_hermann_niangon@yahoo.fr", "22 BP 1227 ABIDJAN 22", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GLORIA SARL.U", "AKA N'DOUBA HERVE", "", "08 BP 3428 ABIDJAN 08", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AKOUPE ZEUDJI", "AKUI AKA MATHIEU", "", "09 BP 1310 ABIDJAN 09", "ANYAMA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE REBOUL", "ALLOU DIAHA CHRISTIANE", "PHCIEreboul@live.fr", "03 BP 3134 ABIDJAN 03", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'ESPOIR", "AMANI ANDRE", "amaniandre@yahoo.fr", "25 BP 620 ABIDJAN 25", "ADJAME/ABIDJAN"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES BOIS", "AMANI FABIENNE EPSE BOSSO", "", "01 BP 1953 ABIDJAN 01", "ABOUDE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SOURALEY", "AMANI ZANTHY GUY ARNAUD", "arnaud_amani@yahoo.fr", "06 BP 2889 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TEHIA", "BROU HERVE", "brouhervebrou@yahoo.fr", "14 BP 1754 ABIDJAN 14", "DABOU-TIASSALE-GD LAHOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("CHU DE COCODY", "CHU DE COCODY", "", "COCODY", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE RAPHA", "DASSE GNAHORE PHILIPPE", "phcieelrapha10@yahoo.fr", "02 BP 251 ABIDJAN 02", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("DION.SVCE.SANTE DES ARMEES", "DIR.SVCE.SANTE DES ARMEE", "", "BP V11 ABIDJAN", "ABIDJAN/PLATEAU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CITE CIE SARLU", "GBEY SIGUI DIOMANDE", "sigui.gbey@gmail.com", "06 BP 1256 ABIDJAN 06", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINTE FAMILLE D'ABENGO", "GNAGBO URBAIN", "dr_gnagbo@yahoo.fr", "BP 1388 ABENGOUROU", "ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAINT JOSEPH ACADEMIE", "KOUADOU AKOUA ADELINE", "adeline.kouadou@yahoo.fr", "21 BP 872 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE LEM", "KOUAKOU KOFFI", "kkoffi69@yahoo.fr", "23 BP 1123 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ZEULAYET SUARL", "KOUAME YAPO VICTOR", "vykouame@yahoo.com", "31 BP 844 ABIDJAN 31", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("TETRA MEDICAL / KOUASSI ANDRE", "KOUASSI ANDRE / TETRA MED", "", "01 BP 125 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE QUARTIER EBRIER SARL", "LAMBIN CHRISTIAN", "phcieebrie@yahoo.fr", "01 BP 121 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE DU MARCHE GOURO SAR", "LOUH VALLE ROGER", "", "21 BP 3024 ABIDJAN 21", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TERESA DE BIEBY", "NIANGORAN ACHO SERGE", "pharmaciebieby@gmail.com", "BP 177 ADZOPE", "ADZOPE/BIEBY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'INDENIE", "N'ZI PILLAH NIALI JACQUEL", "", "01 BP 6746 ABIDJAN 01", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE VALORS", "ORSOT BOSSO VALENTIN", "orsotvalentin@yahoo.fr", "21 BP 643 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHE N'TALE", "OSSEY KOPOIN JUSTIN", "osseykopoinjustin@yahoo.fr", "04 BP 1807 ABIDJAN 04", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU 16ÈME", "SAMECKEN AKPA NIAGNE ARIS", "samecken78@gmail.com", "BP", "ABIDJAN/YOPOUGON SICOGI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE REHOBOTH", "SEKA YAPO CLAUDE RICHARD", "sekayapoclaude@yahoo.fr", "14 BP 89 ABIDJAN 14", "ABIDJAN/ATTECOUBE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BAITY", "SERY LOU MARTHE", "marthesery540@yahoo.fr", "23 BP 4549 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE FLAKIEDOUGOU", "TRAORE BRAHIMA", "", "BP 63 BONDOUKOU", "BONDOUKOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LAFIA", "TRAORE EP.OUATTARA SALIMA", "", "06 BP 218 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES LAUREATS", "YOFFOUA AFFOUE VIRGINIE", "virginie_toure@yahoo.fr", "BP 12 ABENGOUROU", "NIABLE/ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("APPAUL", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("DIVINE GRACE ISSIA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("DIOULABOUGOU SANTE", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("MARCHE GAGNOA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("STE PAULINE", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("DARES SALAM", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        
        // Liste supplémentaire de pharmacies
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ZEULAYET SUARL", "KOUAME YAPO VICTOR", "vykouame@yahoo.com", "31 BP 844 ABIDJAN 31", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("TETRA MEDICAL / KOUASSI ANDRE", "KOUASSI ANDRE / TETRA MED", "", "01 BP 125 ABIDJAN 01", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE QUARTIER EBRIER SARL", "LAMBIN CHRISTIAN", "phcieebrie@yahoo.fr", "01 BP 121 ABIDJAN 01", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE DU MARCHE GOURO SAR", "LOUH VALLE ROGER", "", "21 BP 3024 ABIDJAN 21", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TERESA DE BIEBY", "NIANGORAN ACHO SERGE", "pharmaciebieby@gmail.com", "BP 177 ADZOPE", "ADZOPE/BIEBY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'INDENIE", "N'ZI PILLAH NIALI JACQUEL", "", "01 BP 6746 ABIDJAN 01", "ABIDJAN/ADJAME"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE VALORS", "ORSOT BOSSO VALENTIN", "orsotvalentin@yahoo.fr", "21 BP 643 ABIDJAN 21", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHE N'TALE", "OSSEY KOPOIN JUSTIN", "osseykopoinjustin@yahoo.fr", "04 BP 1807 ABIDJAN 04", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU 16ÈME", "SAMECKEN AKPA NIAGNE ARIS", "samecken78@gmail.com", "BP", "ABIDJAN/YOPOUGON SICOGI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE REHOBOTH", "SEKA YAPO CLAUDE RICHARD", "sekayapoclaude@yahoo.fr", "14 BP 89 ABIDJAN 14", "ABIDJAN/ATTECOUBE"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BAITY", "SERY LOU MARTHE", "marthesery540@yahoo.fr", "23 BP 4549 ABIDJAN 23", "ABIDJAN/YOPOUGON"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE FLAKIEDOUGOU", "TRAORE BRAHIMA", "", "BP 63 BONDOUKOU", "BONDOUKOU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LAFIA", "TRAORE EP.OUATTARA SALIMA", "", "06 BP 218 ABIDJAN 06", "ABIDJAN/COCODY"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES LAUREATS", "YOFFOUA AFFOUE VIRGINIE", "virginie_toure@yahoo.fr", "BP 12 ABENGOUROU", "NIABLE/ABENGOUROU"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE APPAUL", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVINE GRACE ISSIA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIOULABOUGOU SANTE", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARCHE GAGNOA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE STE PAULINE", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DARES SALAM", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUVELLE MARLODIE", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BOKOSSO NOUVELLE", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TENE", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERNE HIRE", "", "", "AXE BOUAFLE-ZUENOULA-GOHITAFLA", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ALLIANCE BOUAFLE", "", "", "AXE VAVOUA-SEGUELA-KANI", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERNE VAVOUA", "", "", "AXE VAVOUA-SEGUELA-KANI", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUVELLE SEGUELA", "", "", "AXE VAVOUA-SEGUELA-KANI", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE SEGUELA", "", "", "AXE VAVOUA-SEGUELA-KANI", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KANI", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE WAHON", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARCHE GUIGLO", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MAIRIE GUIGLO", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LYMANYA", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERNE MAN", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MOSQUE MAN", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KHIANELY", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE COMMERCE MAN", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERNE DANANE", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUVELLE TOUBA", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ROND PONT ODIENNE", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("GRDE PHCIE DU DENGUELE", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FRONTIERE ODIENNE", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ADXONAI D'ORLY", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ANTILOPPE", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SOLEIL", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRINCIPALE DALOA", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FLOREL", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ANEAUD", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE JUBILE", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE STE EMMANUELLA", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GRAND MARCHE", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE COMMERCE DALOA", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GARAGES", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PARVIS", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PROGRES", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MAYESS", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CENTRALE DALOA", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR LABIA", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LOBIA", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MACY-YA", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BEDIALA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUVELLE ESPOIR ISSIA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SEKLE", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MATHE", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CENTRALE GAGNOA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GARAHIO", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHÂTEAU", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRINCIPALE GAGNOA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARLYNS", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AHOUANE", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LAFIA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LAKOTA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KENEYA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FATIMA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE STE MARIE", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARCHE DIVO", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ESPERANCE DIVO", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIALOGUE DIVO", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AMITIE DIVO", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MAIRIE DIVO", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AHOUKOUA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        
        // Liste supplémentaire de pharmacies
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GARE HIRE", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAIOUA", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST JACQUES SAIOUA", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KAVOUDRO", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERNE OURAGAHIO", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST ESPERANCE DIEGONEFLA", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE DIEGONEFLA", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE OUME", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CENTRALE OUME", "", "", "AXE BOUAFLE-ZUENOULA-GOHITAFLA", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR BOUAFLE", "", "", "AXE BOUAFLE-ZUENOULA-GOHITAFLA", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FELY", "", "", "AXE BOUAFLE-ZUENOULA-GOHITAFLA", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PROVIDENCE", "", "", "AXE BOUAFLE-ZUENOULA-GOHITAFLA", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MATHE", "", "", "AXE BOUAFLE-ZUENOULA-GOHITAFLA", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ZUENOULA", "", "", "AXE BOUAFLE-ZUENOULA-GOHITAFLA", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GOHITAFLA", "", "", "AXE VAVOUA-SEGUELA-KANI", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KEJAG", "", "", "AXE VAVOUA-SEGUELA-KANI", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERNE SEGUELA", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GARE DUEKOUE", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIVINE GRACE", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KOKOMAVIE", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ZOUKOUGBEU", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NIKLA", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE GUIGLO", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE BLOLEQUIN", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EL SHADAI TOULEUPLEU", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE BANGOLO", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE COLOMBE MAN", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ODELIA", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PONT DES LIANES", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ESPOIR BIANKOUMA", "", "", "AXE BUYO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE COLOMBE BUYO", "", "", "AXE BUYO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE BUYO", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ROUTE D'ISSIA", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DELORE", "", "", "DALOA VILLE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE HUBERSON", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUVELLE COMMERCE ISSIA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME E LA DELIVRANCE ISSIA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST FRANCOISE D'ASSISE", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GOGNOA", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PHCIE IMMACULE CONCEPTION", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ETAHI", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ZIKISSO", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CENTRALE DIVO", "", "", "AXE ISSIA-GUIBEROUA-GAGNOA-LAKOTA-DIVO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KINNINYA", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRINCIPALE SINFRA", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SINFRA", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GARE SINFRA", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE WATTA", "", "", "AXE SAIOUA-SINFRA-OURAGAHIO-OUME-HIRE-GUEPAHOUO", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUR KADIDJA GUAPAHOUO", "", "", "AXE BOUAFLE-ZUENOULA-GOHITAFLA", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST AUGUSTIN", "", "", "AXE VAVOUA-SEGUELA-KANI", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIETE", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE DUEKOUE", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GUEZON", "", "", "AXE DUEKOUE-GUIGLO-MAN-DANANE-BIANKOUMA-TOUBA-ODIENNE", "DAOLA"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LYCEE CLUB MAN", "", "", "Bouaké", "Bouaké"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARCHE SAKASSOU", "BOUSSIN JAURES", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GBEKE", "COULIBALY DINIGNAKO", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BEOUMI", "KOUAKOU KOUASSI FUL", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE COMMERCE BKE", "KODJO GUY ALBERT TEBAH", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PLACE BKE", "DIOMANDE LASSINA", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE STE MARIE N'GATKRO", "SILUE LANZENY", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MOD KENNEDY", "TOA BI IRIE JUSTIN", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KOKO", "MOUSSOH AMBROISE", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FATIMA", "GNAGBO SERGE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME DE LA SANTE", "KOFFI AMONDJI LOUISE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE YOTIKI", "AMONDJI MOTTOH ARISTIDE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ABOH YAKRO", "KOUADIO BROU PASCAL", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE 220 LOGEMENTS", "KOUASSI KOUADIO EMILE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ND FATIMA YAKRO", "AMOUSSOU GBEDOU HORTENSE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LACS YAKRO", "COWPPLI BONY DANIEL", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE YAMOUSSOUKRO", "AGBASSI MARIE ANTOINETTE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AZI", "ALLAH KOUADIO REMI", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ERGINAL", "EHORA TEHORA", "", "Boucle", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PAIX DAOUKRO", "KOUAKOU ATCHELAUD FAUSTO", "", "Boucle", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ETERNEL OUELLE", "KOUAME NEE YAO N'GUESSAN", "", "Boucle", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SION", "YAVO N'GBESSO CLEMENCE", "", "Boucle", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DAOUKRO", "KRADY REMI BERENGER", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE 14 KGO", "CISSE MAMADOU", "", "Nord", "RCI"));
        
        // Liste supplémentaire de pharmacies (Note: dans cette liste, "Nom" = nom de la pharmacie, "Etablissement" = nom du pharmacien)
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KAKOUMANI", "LAKOUN TOURE", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRINCIPALE KGO", "N'GUESSAN EMMA", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOUR", "DIABATE ANZOUMANAN", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FERKE", "SILUE BARGNANA ERIC", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TCHERGNIM", "KOFFI YAO BA SEVERIN", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NORD KGO", "SADIO KONE", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERN OUANGOLO", "YEO HYASSONGUI DRISSA", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE REHOBOTH", "NANGA VIRGINEIE", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE STE CATHERINE", "BLEHOU JEAN-MICHEL", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EL-NISSI", "YAO YOLANDE EDITH AKISSI", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CARREFOUR ST PIERRE", "N'GOU FRANCK AMOS", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ESPOIR", "SORO FOUGNIGUE", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SOKOURA", "KACOU M.-CLAUDE NEE AKA", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHU BKE", "KOUAME", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CENTRE BKE", "DJIBO YACOUBA SEBASTIEN", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST JEAN BKE", "KOFFI URSULE", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TSF BKE", "TOURE KAFOGNI MOISE", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KENEYA", "DOUMBIA ADAMA", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AVICENNE", "HAIDARA HAMADOU", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE N D DE LA PAIX BKE", "EWUSSI LUCIEN", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARCHE DE GROS", "YEBOUET AMANI JEAN-PIERRE", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE VICTOR TENENA", "YEO TENENA BENJAMIN", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LIBERTE BKE", "YOUIN NORBERT", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE BOTRO", "GNADJA DOGBA RICHARD", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AIR FRANCE", "AMIAH KOUASSI", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST MARC", "TOURE TOFFANH NORBERT", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES CHAMPS", "AKA KADJO JULIEN", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AYATHA", "COULIBALY MAMADOU", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAVANES BKE", "AKPELE ALAIN", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TOUMODI", "ABOU ANOMA EUGENE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ST PAUL DIDIEVI", "AGNIMEL LASME HERVE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE STE MARIE DJEKANOU", "KAUFFY CYRIADE PATRICE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FONDATION", "BERTE SIDIKI", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARIEL", "TETEHI LAGO EMMANUEL", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GRACE DIMBOKRO", "KPLE AHOURE THIERRY", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE RESURRECTION", "GUEBE POIGNONKA HENRI", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DIMBOKRO", "KONAN WAIDHET DANIEL", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MILIWANGAH", "DR AGOLE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SANITAS", "KOUAME BRUNO", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SANS FRONTIERE", "AMANDE APO RAYMONDE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MOROFE", "YEBOUET KOUADIO PRIVAT", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("CLINIQUE MED ENERGY", "YAPO AKISSI RODRIGUE", "", "Boucle", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MBATTO", "YAO KOUASSI MATHIAS", "", "Boucle", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERNE N'ZI", "N'GUESSAN BRUNO", "", "Boucle", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ARRAH", "ATCHELAUD NEE TOURE YVETTE", "", "Boucle", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE BROBO", "ALLA EDMOND", "", "Boucle", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BONGOUANOU", "MAGBI ALAIN", "", "Boucle", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BOCANDA", "OBOU DEMOSTHENE", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GDE MOSQUEE KGO", "MENSAH PIERRE MARIE", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SAVANE NIAKARA", "KOUASSI MICHEL", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MONT KGO", "TRAORE NEE ABOH VIRGINIE", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MARCHE KGO", "BAMBA BAHORATA", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHR KGO", "KOTCHI KASSI", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PALAIS DE JUSTICE", "KOSSA VNCENT DE PAUL", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NAOUSSE", "KONATE BAKARY", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TCHOLOGO", "HOUEDANOU COSME", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NOTRE DAME KGO", "KOTCHI KASSI", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NIENE", "N'GATTA KOUAKOU A.", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FRATERNITE BDLI", "KONE TIESSIBIRI", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DABAKALA", "AGBAN PATRICE", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE NIELLE", "KOFFI AKUE LANDRY", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NASSARA", "OUATTARA MODIBO", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE KATIO", "ALLANGBA HENRI MICHEL", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BEL AIR", "MOUSSA KONE", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CHATEAU BKE", "SIKONGO FOFANGUI", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE STE MADELEINE NIMBO", "GNINIBOU SERY JOACHIM", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DAR ES SALAM", "TOURE JOSEPHINE", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CITE OLYMPIQ", "WEUDJI SERY MATHIEU", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE GDE MOSQUEE BKE", "FOFANA MAMADOU YASSINE", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE CITE BKE", "KOUROUMA KARAMOKO", "", "Bouaké", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ND DE L'ESPERANCE", "KOUYATE CORINNE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE LES MAGNOLIAS", "MANOUAN KOUAME ALFRED", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE STE FAMILLE YAKRO", "N'DA KACOU", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE BELIER", "KASSI NOELLE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MOSQUEE YAKRO", "OLYMPIO BIBIANE", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE ELIASHIB", "KOUASSI DEBRE MICHEL", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE EBEN-EZER", "OULADJOU POHE LUC", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE INDEPENDANCE", "N'TAKPE DAFFOU", "", "Yamoussoukro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE TIEBISSOU", "KOFFI KOUAME ANDRE", "", "Boucle", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE AMITIE DAOUKRO", "KOUASSI", "", "Nord", "RCI"));
        
        // Dernière liste de pharmacies (Note: dans cette liste, "Nom" = nom de la pharmacie, "Etablissement" = nom du pharmacien)
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE SOBA", "COULIBALY GNILMA", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRINCIPALE FKE", "DIBAY HYAHAYA", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE OUANGOLO MARCHE", "GODE KOUDOUBRON", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE FATIMA BDIALI", "KONATE BAKARY", "", "Nord", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE PRINC DIKOUDOUGOU", "TUO KALO ZANO", "jeanarmand2paris@gmail.com / oroz_a@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE MODERNE MEAGUI", "ORO ZEGOU JEAN ARMAND", "phciedesmosquees@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DES MOSQUEES", "AMON ARNAUD", "amaniange18@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE L'IMMACULEE", "ENGOUE WEMBOU EPSE AMANI", "tahdjonan2@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE LA MAIRIE SOUBRE", "TAH BI TAH RIGOBERT", "mecarfour@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU CARREFOUR MEAGUI", "AMANI ZAGARE FULGENCE", "ndakpri@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DU LAC SARL U", "N'DAKPRI BEDIA ANSELME", "lassilandry@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("NVLE PHCIE DE TABOU", "LASSI LATIF LANDRY", "jeanpaul_yao@yahoo.fr / jeanpauldetano@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE SASSANDRA SARL-U", "YAO KOFFI J-P.", "dialloisus@yahoo.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE FRESCO", "DIALLO ISSA", "kouassiyann@yahoo.fr", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE NITORO", "KOUASSI YANN ABDUL", "", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE DE GALEBRE", "BALLET DJEDJE JOSEPH FIRM", "tiakesseh@gmail.com", "San Pédro", "RCI"));
        pharmacies.add(createPharmacyDataFromJsonFormat("PHCIE STE.EMMANUELLE D'OKROUYO", "TIA KESSE HUGUES NARCISSE", "kangadivinepro@gmail.com", "San Pédro", "RCI"));

        return pharmacies;
    }

    /**
     * Classe interne pour représenter les données d'une pharmacie
     */
    private static class PharmacyData {
        String name;
        String phone1;
        String phone2;
        String email;
        String horaires;
        String city;
        String commune;
        String district;
        String description;
        String pharmacistName;
        String photo;
        Double latitude;
        Double longitude;

        PharmacyData(String name, String phone1, String phone2, String email, String horaires,
                     String city, String commune, String district, String description,
                     String pharmacistName, String photo,
                     Double latitude, Double longitude) {
            this.name = name;
            this.phone1 = phone1;
            this.phone2 = phone2;
            this.email = email;
            this.horaires = horaires;
            this.city = city;
            this.commune = commune;
            this.district = district;
            this.description = description;
            this.pharmacistName = pharmacistName;
            this.photo = photo;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}

