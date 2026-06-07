PROCÉDURE DE TEST DE CONTINUITÉ D’ACTIVITÉ (PCA)
Rôle : DBA – Environnements ODS & SYRTA
 
⸻
 
1. Contexte et objectifs du test
Le présent test de continuité d’activité vise à évaluer la capacité de l’infrastructure SI à maintenir la disponibilité des applications critiques en cas d’indisponibilité soudaine du site principal.
Objectifs principaux
* Valider la résilience des services en cas de perte totale du site principal DATA ROOM Bab Ezzouar.
* S’assurer que l’activité de la Direction des Opérations puisse se poursuivre sans interruption majeure.
* Confirmer la disponibilité et l’intégrité des données sensibles après bascule vers le site de repli DATA ROOM Cheraga.
* Tester les mécanismes de réplication, bascule applicative et base de données.
 
⸻
 
2. Périmètre du test (Cible)
Sites concernés
* Site principal : DATA ROOM Bab Ezzouar
* Site de repli : DATA ROOM Cheraga
Solutions métiers impactées
* SYRTA
* ODS
(table des serveurs telle que fournie – à conserver dans le document final)
 
⸻
 
3. Pré-requis et préparations DBA
Avant le déclenchement du test, les actions suivantes ont été réalisées :
3.1 Vérifications préalables
* Vérification de l’état de synchronisation des bases standby (ODS & SYRTA).
* Vérification de l’accessibilité des portails utilisateurs sur le site principal.
* Confirmation de l’espace disque et de la santé des bases standby.
3.2 Génération des derniers journaux
Afin de garantir la synchronisation maximale avant l’arrêt brutal :
ALTER SYSTEM SWITCH LOGFILE;
Commande exécutée sur les bases :
* ODS : serveurs 109 / 110
* SYRTA : serveurs 309 / 310
 
⸻
 
4. Déclenchement manuel des défaillances (Simulation sinistre)
4.1 Actions infrastructure & applicatives
* Arrêt des services Tomcat sur :
    * ODS APP : algs07200109
    * SYRTA APP : algs07200309
* Réalisation de snapshots des serveurs standby :
    * SYRTA : 509 / 510
    * ODS : 609 / 610
* Déconnexion des interfaces réseau des serveurs principaux :
    * 109, 110, 309, 310
4.2 Vérifications réseau
* Ping des serveurs et URLs applicatives : 
    * État OK avant déconnexion
    * Interruption totale après coupure réseau 📎 Captures d’écran jointes

 
⸻
 
5. Résultats constatés à chaud (Post-défaillance)
Composant	Impact
ODS portail principal	Indisponible
SYRTA portail principal	Indisponible
Tests utilisateurs réalisés :
* https://ods-dz-assurance.is.echonet/ ❌
* https://syrtadz.drp-assurance.is.echonet/ ❌ 📎 Captures navigateur jointes
 
⸻
 
6. Déclenchement du PCA – Site de repli
6.1 Infrastructure de repli activée
Les mécanismes de résilience suivants ont été mis en œuvre :
Application	Mécanisme	Serveur backup	Synchronisation	Déclencheur
ODS APP	Standby	algs07200609	Temps réel	DBA
ODS BDD	Data Guard	algs07200610	Temps réel	DBA
SYRTA APP	Standby	algs07200509	Temps réel	DBA
SYRTA BDD	Standby	algs07200510	Temps réel	DBA


7. Procédure DBA – Bascule ODS (Data Guard)

7.1 Vérification du rôle et de l’état de la base standby

SELECT name, open_mode, database_role FROM v$database;

Résultat attendu :
DATABASE_ROLE : PHYSICAL STANDBY
OPEN_MODE     : MOUNTED
 
⸻
 


7.2 Arrêt du processus de récupération (MRP)


RECOVER MANAGED STANDBY DATABASE CANCEL;
Résultat :


Media recovery complete.
 
⸻
 




7.3 Finalisation de l’application des journaux


ALTER DATABASE RECOVER MANAGED STANDBY DATABASE FINISH;

 
⸻
 


7.4 Activation de la base standby en base primaire


ALTER DATABASE ACTIVATE STANDBY DATABASE;
 
⸻
 


7.5 Ouverture de la base en production


ALTER DATABASE OPEN;
 
⸻
 



7.6 Vérification finale


SELECT name, open_mode, database_role FROM v$database;


Résultat attendu :
DATABASE_ROLE : PRIMARY
OPEN_MODE     : READ WRITE
 
⸻





 
8. Procédure DBA – Bascule SYRTA (Standby Database)
La base SYRTA dispose d’une base standby synchronisée en temps réel, sans Data Guard Broker. La bascule est réalisée manuellement par le DBA.
 
⸻



 
8.1 Vérification du rôle de la base standby SYRTA


SELECT name, open_mode, database_role FROM v$database;


Résultat attendu :
DATABASE_ROLE : PHYSICAL STANDBY
OPEN_MODE     : MOUNTED
 
⸻



 
8.2 Arrêt du processus de récupération (MRP)


RECOVER MANAGED STANDBY DATABASE CANCEL;
 
⸻


 
8.3 Finalisation de l’application des redo logs


ALTER DATABASE RECOVER MANAGED STANDBY DATABASE FINISH;
 
⸻



8.4 Activation de la base standby SYRTA


ALTER DATABASE ACTIVATE STANDBY DATABASE;
 
⸻


 
8.5 Ouverture de la base en mode production


ALTER DATABASE OPEN;


 
⸻
 
8.6 Vérification post-bascule


SELECT name, open_mode, database_role FROM v$database;


Résultat attendu :
DATABASE_ROLE : PRIMARY
OPEN_MODE     : READ WRITE
 
⸻



 
9. Validation applicative post-bascule
* Redirection des flux applicatifs vers le site DATA ROOM Cheraga
* Vérification des URLs :
    * SYRTA : https://syrtadz.drp-assurance.is.echonet/ords
    * ODS : https://odsdg-dz-assurance.is.echonet/ords
* Accès utilisateurs validés
* Absence de perte de données constatée
📎 Captures navigateur jointes
 
⸻


 
10. Procédure de Retour à la Normale (Failback)
10.1 Objectif
Restaurer l’architecture nominale sur le site principal DATA ROOM Bab Ezzouar, tout en garantissant l’intégrité des données et la continuité applicative.


 
⸻



 
10.2 Restauration des serveurs à partir des snapshots
Actions réalisées :
* Restauration des snapshots des serveurs BDD principaux :
    * SYRTA BDD : algs07200310
    * ODS BDD : algs07200110
* Restauration des snapshots des serveurs BDD de repli :
    * SYRTA Standby BDD : algs07200510
    * ODS Standby BDD : algs07200610
✔️ Objectif : garantir une cohérence complète entre les environnements primaire et standby avant la remise en production.
 
⸻
 
10.3 Reconfiguration des rôles des bases de données
10.3.1 Remise en primaire des bases du site principal
* Démarrage des bases restaurées sur Bab Ezzouar
* Vérification du rôle :
SELECT name, open_mode, database_role FROM v$database;
Résultat attendu :
DATABASE_ROLE : PRIMARY
 
⸻
 
10.3.2 Recréation / resynchronisation des bases standby
* Démarrage des bases standby sur Cheraga
* Mise en place de la synchronisation (redo apply)
RECOVER MANAGED STANDBY DATABASE USING CURRENT LOGFILE DISCONNECT;
 
⸻
 
10.4 Redémarrage des services applicatifs (Production)
Serveurs concernés
* ODS APP : algs07200109
* SYRTA APP : algs07200309
Commandes de redémarrage Tomcat
systemctl start tomcat
ou selon environnement :
service tomcat start
 
⸻
 
10.5 Vérifications post-retour à la normale
* Accessibilité des portails utilisateurs depuis le site principal :
    * https://ods-dz-assurance.is.echonet/ords
    * https://syrta-dz-assurance.is.echonet/ords
* Vérification des connexions applicatives aux bases primaires
* Contrôle de la synchronisation standby
* Validation fonctionnelle par les utilisateurs métier
 
⸻
 
11. Conclusion DBA
Le test PCA a permis de valider :
* La capacité de bascule contrôlée des bases ODS (Data Guard) et SYRTA (Standby).
* La disponibilité des services depuis le site de repli sans perte de données.
* La maîtrise complète du failover et du failback par les équipes DBA et OPS.
L’architecture actuelle répond aux exigences de continuité d’activité, avec un RPO et RTO conformes aux attentes métier.
