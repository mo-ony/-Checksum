Parfait, Fatima ! Ce que tu veux, c’est un **script de migration automatique** qui se lance **une seule fois au démarrage de l’application**, pour calculer le checksum des documents déjà existants. Ensuite, tu peux le retirer.

Voici comment faire proprement avec **Spring Boot** :

---

### 1️⃣ Créer un `CommandLineRunner` pour l’exécution au démarrage

Spring Boot exécute tous les `CommandLineRunner` après le démarrage du contexte.

```java
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Blob;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Component
public class ChecksumMigrationRunner implements CommandLineRunner {

    @Autowired
    private SaveRepository archivageRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Migration checksum démarrée ===");

        List<ArchivageEntity> allDocs = archivageRepository.findAll();

        for (ArchivageEntity doc : allDocs) {
            // Vérifier si checksum déjà rempli
            if (doc.getChecksum() != null && !doc.getChecksum().isEmpty()) continue;

            // Lire le BLOB
            Blob blob = doc.getContrat();
            byte[] fileBytes = blob.getBytes(1, (int) blob.length());

            // Calculer checksum SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            String checksum = HexFormat.of().formatHex(hash);

            // Mettre à jour l'entité
            doc.setChecksum(checksum);
            archivageRepository.save(doc);

            System.out.println("Checksum calculé pour : " + doc.getNumPolice());
        }

        System.out.println("=== Migration checksum terminée ===");
    }
}
```

---

### 2️⃣ Explications

* **Se lance automatiquement** au démarrage grâce à `CommandLineRunner`.
* **Vérifie si le champ checksum est déjà rempli** pour ne pas recalculer inutilement.
* Lit le **BLOB du document**, calcule le **SHA-256**, et met à jour l’entité.
* Après exécution, tu peux **supprimer ce `CommandLineRunner`** de ton projet.

---

### 3️⃣ Bonus

Si tu veux éviter le `save` un par un (optimisation), tu peux :

* Traiter en **batch**, par exemple 50 documents à la fois.
* Désactiver les logs SQL temporaires pour accélérer le process.

---

Si tu veux, je peux te faire une **version “batch et transactionnel”**, plus adaptée si ta GED a **des milliers de fichiers**.

Veux‑tu que je fasse ça ?
