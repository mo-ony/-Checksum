Parfait, Fatima. On va **réadapter ton backend pour inclure le checksum** lors de l’import, en gardant ton architecture existante (Spring Boot + Oracle + Base64 → BLOB). Je vais te proposer une version complète et propre de ton service et de ton entity.

---

### 1️⃣ Ajout du champ checksum dans l’entity

```java
@Column(name = "CHECKSUM", nullable = false, length = 64, updatable = false)
private String checksum;

@Column(name = "CHECKSUM_ALGO", nullable = false, length = 10, updatable = false)
private String checksumAlgo = "SHA-256";

public void setChecksum(String checksum) {
    this.checksum = checksum;
}
```

---

### 2️⃣ Calcul du checksum dans le service

On calcule le SHA-256 directement à partir du BLOB du fichier avant de l’enregistrer :

```java
import java.security.MessageDigest;
import java.util.HexFormat;

public static String calculateChecksum(byte[] fileBytes) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(fileBytes);
    return HexFormat.of().formatHex(hash);
}
```

---

### 3️⃣ Modification de `saveContracts` pour inclure le checksum

```java
@Transactional
public void saveContracts(List<ArchiveDTO> contracts) throws SQLException {
    for (ArchiveDTO contract : contracts) {
        // Vérifier unicité
        Optional<ArchivageEntity> existingContract = archivageRepository
                .findByNumPoliceAndNomAndPrenom(contract.getNumPolice(), contract.getNom(), contract.getPrenom());

        if (existingContract.isPresent()) {
            throw new RuntimeException("Le contrat avec le numéro " + contract.getNumPolice() + " existe déjà !");
        }

        // Vérifier PDF
        if (!isPdf(contract.getContratBase64())) {
            throw new RuntimeException("Seuls les fichiers PDF sont autorisés !");
        }

        // Base64 → byte[]
        byte[] contratBytes = Base64.getDecoder().decode(contract.getContratBase64());

        // Calcul checksum
        String checksum;
        try {
            checksum = calculateChecksum(contratBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du calcul du checksum", e);
        }

        // Mapper DTO → Entity
        ArchivageEntity entity = new ArchivageEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setNumPolice(contract.getNumPolice());
        entity.setNom(contract.getNom());
        entity.setPrenom(contract.getPrenom());
        entity.setNjf(contract.getNjf());
        entity.setAnnee(contract.getAnnee());
        entity.setMois(contract.getMois());
        entity.setProduitId(contract.getProduitId());
        entity.setArchivePar(contract.getArchivePar());
        entity.setMontantAssure(contract.getMontantAssure());
        entity.setMontantPrime(contract.getMontantPrime());
        entity.setContrat(new SerialBlob(contratBytes));
        entity.setDateNaissance(contract.getDateNaisssance());
        entity.setDateEffet(contract.getDateEffet());
        entity.setFlag(contract.getFlag());

        // Set checksum
        entity.setChecksum(checksum);

        // Enregistrer
        archivageRepository.save(entity);
    }
}
```

---

### 4️⃣ Notes importantes

1. **Stockage SHA-256** → 64 caractères hexadécimal dans Oracle.
2. **Recalculable** → à tout moment pour vérifier intégrité.
3. **Indépendant de la source client** → toujours calculé côté backend.
4. **Audit / versioning / doublons** → le checksum peut servir à détecter les doublons et vérifier les versions.

---

Si tu veux, je peux te proposer **une version encore plus optimisée pour de gros fichiers** où le checksum est calculé en streaming pour éviter de charger tout le fichier en mémoire.

Veux‑tu que je fasse ça ?
