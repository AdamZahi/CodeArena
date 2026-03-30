package com.codearena.module8_terminalquest;

import com.codearena.module8_terminalquest.entity.StoryChapter;
import com.codearena.module8_terminalquest.entity.StoryMission;
import com.codearena.module8_terminalquest.repository.StoryChapterRepository;
import com.codearena.module8_terminalquest.repository.StoryMissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StoryChapterRepository chapterRepository;
    private final StoryMissionRepository missionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (chapterRepository.count() > 0) {
            log.info("Story chapters already seeded — skipping DataInitializer.");
            return;
        }
        log.info("Seeding Terminal Quest story chapters and missions...");
        seedChapter1();
        seedChapter2();
        seedChapter3();
        seedChapter4();
        log.info("Seeding complete.");
    }

    // ── CHAPTER 1 ─────────────────────────────────────────────────────────────

    private void seedChapter1() {
        StoryChapter ch = chapterRepository.save(StoryChapter.builder()
                .title("Jour 1 : Bienvenue chez NexaTech")
                .description("Ton premier jour en tant que DevOps junior. Apprends les commandes de base pour naviguer sur le serveur.")
                .orderIndex(1)
                .isLocked(false)
                .build());

        saveMission(ch, 1, "Où suis-je ?",
                "C'est ton premier jour chez NexaTech. Le lead DevOps t'accueille : 'Bienvenue ! On va commencer doucement. Connecte-toi au serveur de dev et dis-moi où tu te trouves.'",
                "Affiche le répertoire courant",
                "[\"pwd\"]",
                "La commande pwd affiche le répertoire de travail actuel",
                "EASY", false, 25);

        saveMission(ch, 2, "Explorer les logs",
                "'Bien joué ! Maintenant j'ai besoin que tu regardes les logs système. Va voir ce qu'il y a dans le dossier /var/log.'",
                "Liste le contenu de /var/log",
                "[\"ls /var/log\", \"ls -la /var/log\", \"ls -l /var/log\", \"ls -al /var/log\"]",
                "La commande ls liste les fichiers d'un répertoire",
                "EASY", false, 25);

        saveMission(ch, 3, "Lire la config",
                "'Super. Le client veut savoir le hostname du serveur. Vérifie ça pour moi.'",
                "Affiche le contenu du fichier /etc/hostname",
                "[\"cat /etc/hostname\"]",
                "La commande cat affiche le contenu d'un fichier",
                "EASY", false, 50);

        saveMission(ch, 4, "URGENCE — Site web down !",
                "ALERTE ROUGE ! Le monitoring vient de sonner. Le site web de NexaTech est inaccessible ! Le lead DevOps court vers toi : 'Le site est down ! J'ai besoin que tu vérifies immédiatement les dernières lignes du log d'erreur nginx. VITE !'",
                "Affiche les 20 dernières lignes de /var/log/nginx/error.log",
                "[\"tail -20 /var/log/nginx/error.log\", \"tail -n 20 /var/log/nginx/error.log\", \"tail -n20 /var/log/nginx/error.log\"]",
                "La commande tail affiche la fin d'un fichier, l'option -n spécifie le nombre de lignes",
                "HARD", true, 150);
    }

    // ── CHAPTER 2 ─────────────────────────────────────────────────────────────

    private void seedChapter2() {
        StoryChapter ch = chapterRepository.save(StoryChapter.builder()
                .title("L'incident du serveur web")
                .description("Le serveur nginx tombe en panne. Diagnostique et répare le service avant que les clients ne se plaignent trop.")
                .orderIndex(2)
                .isLocked(true)
                .build());

        saveMission(ch, 1, "Diagnostic du service",
                "Lendemain matin. Le lead t'appelle : 'On a encore des problèmes avec nginx. Les clients se plaignent. Vérifie si le service tourne.'",
                "Vérifie le statut du service nginx",
                "[\"systemctl status nginx\", \"service nginx status\"]",
                "La commande systemctl status <service> vérifie l'état d'un service",
                "EASY", false, 50);

        saveMission(ch, 2, "Redémarrage d'urgence",
                "'Comme je le pensais, le service est arrêté. Redémarre-le tout de suite !'",
                "Redémarre le service nginx",
                "[\"systemctl restart nginx\", \"systemctl start nginx\", \"service nginx restart\", \"service nginx start\"]",
                "systemctl restart arrête puis redémarre le service",
                "MEDIUM", false, 75);

        saveMission(ch, 3, "Vérification du port",
                "'OK il a redémarré. Maintenant vérifie que le port 80 est bien en écoute. On doit être sûrs.'",
                "Vérifie que le port 80 est ouvert",
                "[\"netstat -tlnp | grep 80\", \"ss -tlnp | grep 80\", \"netstat -tlnp | grep :80\", \"ss -tlnp | grep :80\"]",
                "ss ou netstat avec l'option -tlnp liste les ports en écoute",
                "MEDIUM", false, 75);

        saveMission(ch, 4, "BOSS — Le port est occupé !",
                "CATASTROPHE ! Nginx refuse de démarrer car le port 80 est déjà utilisé par un autre processus. Le CTO arrive et demande : 'Trouve-moi IMMÉDIATEMENT quel processus squatte le port 80 !'",
                "Trouve quel processus utilise le port 80",
                "[\"lsof -i :80\", \"fuser 80/tcp\", \"netstat -tlnp | grep :80\", \"ss -tlnp | grep :80\"]",
                "lsof -i :80 liste les processus utilisant le port 80",
                "HARD", true, 200);
    }

    // ── CHAPTER 3 ─────────────────────────────────────────────────────────────

    private void seedChapter3() {
        StoryChapter ch = chapterRepository.save(StoryChapter.builder()
                .title("Intrusion détectée")
                .description("L'équipe sécurité a détecté une activité suspecte. Enquête sur l'intrusion et sécurise le serveur.")
                .orderIndex(3)
                .isLocked(true)
                .build());

        saveMission(ch, 1, "Qui est connecté ?",
                "L'équipe sécurité a détecté une activité suspecte sur le serveur de production. Le responsable sécurité te dit : 'On a une possible intrusion. Vérifie qui est actuellement connecté au serveur.'",
                "Affiche les utilisateurs connectés",
                "[\"who\", \"w\", \"users\"]",
                "La commande who ou w liste les utilisateurs actuellement connectés",
                "EASY", false, 50);

        saveMission(ch, 2, "Traquer l'intrus",
                "'Il y a un utilisateur que je ne reconnais pas. Cherche dans les logs SSH les tentatives de connexion échouées. Ça nous donnera l'IP de l'attaquant.'",
                "Cherche les échecs de connexion SSH dans les logs",
                "[\"grep 'Failed' /var/log/auth.log\", \"grep 'Failed password' /var/log/auth.log\", \"cat /var/log/auth.log | grep Failed\"]",
                "Les tentatives SSH échouées sont loguées avec 'Failed password' dans /var/log/auth.log",
                "MEDIUM", false, 75);

        saveMission(ch, 3, "Bloquer l'IP",
                "'On a trouvé l'IP ! C'est 192.168.1.100. Bloque cette IP dans le firewall immédiatement, avant qu'il ne cause plus de dégâts.'",
                "Bloque l'IP 192.168.1.100 dans le firewall",
                "[\"iptables -A INPUT -s 192.168.1.100 -j DROP\", \"ufw deny from 192.168.1.100\"]",
                "iptables ou ufw permettent de bloquer des IPs au niveau du firewall",
                "HARD", false, 100);

        saveMission(ch, 4, "BOSS — Éliminer la backdoor",
                "ALERTE CRITIQUE ! L'intrus a créé un compte backdoor sur le serveur. Le CISO hurle : 'Il a créé un user appelé hacker ! Supprime ce compte MAINTENANT avant qu'il ne revienne !'",
                "Supprime le compte utilisateur 'hacker'",
                "[\"userdel hacker\", \"userdel -r hacker\", \"deluser hacker\"]",
                "userdel supprime un compte utilisateur, -r supprime aussi son répertoire home",
                "HARD", true, 200);
    }

    // ── CHAPTER 4 ─────────────────────────────────────────────────────────────

    private void seedChapter4() {
        StoryChapter ch = chapterRepository.save(StoryChapter.builder()
                .title("Le disque plein")
                .description("Le serveur de production est à 98% de capacité disque. Trouve et libère de l'espace avant le crash.")
                .orderIndex(4)
                .isLocked(true)
                .build());

        saveMission(ch, 1, "État du disque",
                "Lundi matin. Toutes les alertes sont rouges. Le serveur de production est à 98% d'espace disque. Ton manager : 'Le serveur va crasher si on ne fait rien. Vérifie l'espace disque.'",
                "Vérifie l'espace disque",
                "[\"df -h\", \"df -h /\"]",
                "df -h affiche l'utilisation du disque en format lisible (human-readable)",
                "EASY", false, 50);

        saveMission(ch, 2, "Trouver le coupable",
                "'Le disque /var est presque plein. Trouve quels dossiers prennent le plus de place.'",
                "Analyse l'utilisation disque dans /var",
                "[\"du -sh /var/*\", \"du -sh /var/* | sort -rh\", \"du -ah /var | sort -rh | head\"]",
                "du -sh donne la taille de chaque sous-dossier, sort -rh trie du plus grand au plus petit",
                "MEDIUM", false, 75);

        saveMission(ch, 3, "Compresser les logs",
                "'Ce sont les vieux logs ! Le fichier /var/log/app.log.old fait 5Go. Compresse-le pour gagner de la place.'",
                "Compresse le fichier /var/log/app.log.old",
                "[\"gzip /var/log/app.log.old\", \"bzip2 /var/log/app.log.old\", \"xz /var/log/app.log.old\"]",
                "gzip, bzip2 et xz sont des outils de compression de fichiers",
                "MEDIUM", false, 75);

        saveMission(ch, 4, "BOSS — Nettoyage total",
                "LE SERVEUR VA TOMBER ! Il reste 1% d'espace. Le CTO est en panique : 'Trouve et supprime TOUS les fichiers .tmp de plus de 100Mo sur tout le serveur. C'est notre dernière chance !'",
                "Trouve et supprime tous les .tmp > 100Mo",
                "[\"find / -name '*.tmp' -size +100M -delete\", \"find / -type f -name '*.tmp' -size +100M -delete\", \"find / -name '*.tmp' -size +100M -exec rm {} \\\\;\", \"find / -type f -name '*.tmp' -size +100M -exec rm {} +\"]",
                "find avec -size +100M cherche les fichiers de plus de 100Mo, -delete ou -exec rm les supprime",
                "HARD", true, 300);
    }

    // ── HELPER ────────────────────────────────────────────────────────────────

    private void saveMission(StoryChapter chapter, int order, String title,
                              String context, String task, String acceptedAnswers,
                              String hint, String difficulty, boolean isBoss, int xpReward) {
        missionRepository.save(StoryMission.builder()
                .chapter(chapter)
                .orderIndex(order)
                .title(title)
                .context(context)
                .task(task)
                .acceptedAnswers(acceptedAnswers)
                .hint(hint)
                .difficulty(difficulty)
                .isBoss(isBoss)
                .xpReward(xpReward)
                .build());
    }
}
