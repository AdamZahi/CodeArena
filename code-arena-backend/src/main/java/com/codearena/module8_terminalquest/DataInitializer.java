package com.codearena.module8_terminalquest;

import com.codearena.module8_terminalquest.entity.StoryChapter;
import com.codearena.module8_terminalquest.entity.StoryMission;
import com.codearena.module8_terminalquest.repository.LevelProgressRepository;
import com.codearena.module8_terminalquest.repository.StoryChapterRepository;
import com.codearena.module8_terminalquest.repository.StoryMissionRepository;
import com.codearena.module8_terminalquest.tts.SpeakerRotation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final StoryChapterRepository chapterRepository;
    private final StoryMissionRepository missionRepository;
    private final LevelProgressRepository levelProgressRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<StoryChapter> chapters = chapterRepository.findAllByOrderByOrderIndexAsc();

        if (chapters.isEmpty()) {
            log.info("No data found — seeding Terminal Quest story data...");
            seedAll();
            return;
        }

        // If any chapter is missing its speaker, wipe everything and re-seed cleanly.
        // Delete LevelProgress first (FK → StoryMission / StoryLevel), then chapters
        // (cascade deletes missions + levels automatically).
        boolean needsReseed = chapters.stream().anyMatch(c -> c.getSpeakerName() == null);
        if (needsReseed) {
            log.info("Outdated seed data detected — wiping and re-seeding...");
            levelProgressRepository.deleteAll();
            chapterRepository.deleteAll(); // cascades to missions + levels
            seedAll();
            return;
        }

        log.info("Story data already up-to-date — skipping DataInitializer.");
    }

    // ── full seed ─────────────────────────────────────────────────────────────

    private void seedAll() {
        seedChapter1();
        seedChapter2();
        seedChapter3();
        seedChapter4();
        log.info("Seeding complete.");
    }

    // ── Chapter 1 — Sarah Chen / Aoede ────────────────────────────────────────

    private void seedChapter1() {
        SpeakerRotation.Speaker sp = SpeakerRotation.getSpeakerForChapter(1); // Sarah Chen / Aoede
        StoryChapter ch = chapterRepository.save(StoryChapter.builder()
                .title("Jour 1 : Bienvenue chez NexaTech")
                .description("Ton premier jour en tant que DevOps junior. Apprends les commandes de base pour naviguer sur le serveur.")
                .orderIndex(1)
                .isLocked(false)
                .speakerName(sp.name())
                .speakerVoice(sp.voice())
                .build());

        saveMission(ch, 1, "Où suis-je ?",
                "Hey, bienvenue dans l'équipe ! Je suis Sarah, la lead DevOps. On va y aller doucement pour ton premier jour. Connecte-toi au serveur de dev et dis-moi dans quel répertoire tu te trouves.",
                "Affiche le répertoire courant",
                "[\"pwd\"]",
                "La commande pwd affiche le répertoire de travail actuel",
                "EASY", false, 25);

        saveMission(ch, 2, "Explorer les logs",
                "Bien joué ! OK, maintenant j'ai besoin que tu jettes un oeil aux logs système. Va voir ce qu'il y a dans le dossier /var/log et dis-moi ce que tu trouves.",
                "Liste le contenu de /var/log",
                "[\"ls /var/log\", \"ls -la /var/log\", \"ls -l /var/log\", \"ls -al /var/log\"]",
                "La commande ls liste les fichiers d'un répertoire",
                "EASY", false, 25);

        saveMission(ch, 3, "Lire la config",
                "Super, tu t'en sors bien ! Le client nous demande le hostname de ce serveur. Tu peux me l'afficher rapidement ?",
                "Affiche le contenu du fichier /etc/hostname",
                "[\"cat /etc/hostname\"]",
                "La commande cat affiche le contenu d'un fichier",
                "EASY", false, 50);

        saveMission(ch, 4, "URGENCE — Site web down !",
                "URGENCE ! Le monitoring vient de sonner, le site est complètement down ! J'ai besoin que tu vérifies les dernières lignes du log d'erreur nginx immédiatement. On a des clients qui appellent, DÉPÊCHE-TOI !",
                "Affiche les 20 dernières lignes de /var/log/nginx/error.log",
                "[\"tail -20 /var/log/nginx/error.log\", \"tail -n 20 /var/log/nginx/error.log\", \"tail -n20 /var/log/nginx/error.log\"]",
                "La commande tail affiche la fin d'un fichier, l'option -n spécifie le nombre de lignes",
                "HARD", true, 150);
    }

    // ── Chapter 2 — Lina Torres / Kore ────────────────────────────────────────

    private void seedChapter2() {
        SpeakerRotation.Speaker sp = SpeakerRotation.getSpeakerForChapter(2); // Lina Torres / Kore
        StoryChapter ch = chapterRepository.save(StoryChapter.builder()
                .title("L'incident du serveur web")
                .description("Le serveur nginx tombe en panne. Diagnostique et répare le service avant que les clients ne se plaignent trop.")
                .orderIndex(2)
                .isLocked(true)
                .speakerName(sp.name())
                .speakerVoice(sp.voice())
                .build());

        saveMission(ch, 1, "Diagnostic du service",
                "Salut, c'est Lina, architecte cloud ici. On a des soucis avec nginx ce matin, les clients se plaignent que le site rame. Vérifie si le service tourne correctement.",
                "Vérifie le statut du service nginx",
                "[\"systemctl status nginx\", \"service nginx status\"]",
                "La commande systemctl status <service> vérifie l'état d'un service",
                "EASY", false, 50);

        saveMission(ch, 2, "Redémarrage d'urgence",
                "Comme je le pensais, le service est arrêté. Redémarre-le tout de suite, chaque minute qui passe nous coûte des utilisateurs.",
                "Redémarre le service nginx",
                "[\"systemctl restart nginx\", \"systemctl start nginx\", \"service nginx restart\", \"service nginx start\"]",
                "systemctl restart arrête puis redémarre le service",
                "MEDIUM", false, 75);

        saveMission(ch, 3, "Vérification du port",
                "OK il a l'air d'avoir redémarré. Mais vérifie que le port 80 est bien en écoute, je veux être sûre à cent pour cent avant de dire au client que c'est réglé.",
                "Vérifie que le port 80 est ouvert",
                "[\"netstat -tlnp | grep 80\", \"ss -tlnp | grep 80\", \"netstat -tlnp | grep :80\", \"ss -tlnp | grep :80\"]",
                "ss ou netstat avec l'option -tlnp liste les ports en écoute",
                "MEDIUM", false, 75);

        saveMission(ch, 4, "BOSS — Le port est occupé !",
                "CATASTROPHE ! Nginx refuse de démarrer, le port 80 est déjà pris par un autre processus ! Le Directeur vient de m'appeler, il est furieux. Trouve-moi IMMÉDIATEMENT quel processus squatte le port 80 !",
                "Trouve quel processus utilise le port 80",
                "[\"lsof -i :80\", \"fuser 80/tcp\", \"netstat -tlnp | grep :80\", \"ss -tlnp | grep :80\"]",
                "lsof -i :80 liste les processus utilisant le port 80",
                "HARD", true, 200);
    }

    // ── Chapter 3 — Alex Rivera / Puck ────────────────────────────────────────

    private void seedChapter3() {
        SpeakerRotation.Speaker sp = SpeakerRotation.getSpeakerForChapter(3); // Alex Rivera / Puck
        StoryChapter ch = chapterRepository.save(StoryChapter.builder()
                .title("Intrusion détectée")
                .description("L'équipe sécurité a détecté une activité suspecte. Enquête sur l'intrusion et sécurise le serveur.")
                .orderIndex(3)
                .isLocked(true)
                .speakerName(sp.name())
                .speakerVoice(sp.voice())
                .build());

        saveMission(ch, 1, "Qui est connecté ?",
                "Hey, c'est Alex, SRE engineer. On a détecté une activité suspecte sur le serveur de production. J'ai besoin que tu me dises immédiatement qui est actuellement connecté.",
                "Affiche les utilisateurs connectés",
                "[\"who\", \"w\", \"users\"]",
                "La commande who ou w liste les utilisateurs actuellement connectés",
                "EASY", false, 50);

        saveMission(ch, 2, "Traquer l'intrus",
                "Il y a un utilisateur que je ne reconnais pas dans la liste. Fouille les logs SSH et cherche les tentatives de connexion échouées. Ça nous donnera l'adresse IP de l'attaquant.",
                "Cherche les échecs de connexion SSH dans les logs",
                "[\"grep 'Failed' /var/log/auth.log\", \"grep 'Failed password' /var/log/auth.log\", \"cat /var/log/auth.log | grep Failed\"]",
                "Les tentatives SSH échouées sont loguées avec 'Failed password' dans /var/log/auth.log",
                "MEDIUM", false, 75);

        saveMission(ch, 3, "Bloquer l'IP",
                "On a identifié l'IP : 192.168.1.100. Bloque cette adresse dans le firewall immédiatement. Chaque seconde compte, il pourrait être en train de voler des données.",
                "Bloque l'IP 192.168.1.100 dans le firewall",
                "[\"iptables -A INPUT -s 192.168.1.100 -j DROP\", \"ufw deny from 192.168.1.100\"]",
                "iptables ou ufw permettent de bloquer des IPs au niveau du firewall",
                "HARD", false, 100);

        saveMission(ch, 4, "BOSS — Éliminer la backdoor",
                "ALERTE CRITIQUE ! L'intrus a créé un compte backdoor sur notre serveur ! Un utilisateur appelé hacker vient d'apparaître. Supprime ce compte MAINTENANT avant qu'il ne revienne et cause des dégâts irréversibles !",
                "Supprime le compte utilisateur 'hacker'",
                "[\"userdel hacker\", \"userdel -r hacker\", \"deluser hacker\"]",
                "userdel supprime un compte utilisateur, -r supprime aussi son répertoire home",
                "HARD", true, 200);
    }

    // ── Chapter 4 — Nadia Park / Aoede (boss → Le Directeur / Puck) ───────────

    private void seedChapter4() {
        SpeakerRotation.Speaker sp = SpeakerRotation.getSpeakerForChapter(4); // Nadia Park / Aoede
        StoryChapter ch = chapterRepository.save(StoryChapter.builder()
                .title("Le disque plein")
                .description("Le serveur de production est à 98% de capacité disque. Trouve et libère de l'espace avant le crash.")
                .orderIndex(4)
                .isLocked(true)
                .speakerName(sp.name())
                .speakerVoice(sp.voice())
                .build());

        saveMission(ch, 1, "État du disque",
                "Ici Nadia, analyste sécurité. Mauvaise nouvelle ce matin, toutes les alertes sont au rouge. Le serveur de production est à 98 pourcent de capacité disque. Vérifie l'espace disque et dis-moi la situation exacte.",
                "Vérifie l'espace disque",
                "[\"df -h\", \"df -h /\"]",
                "df -h affiche l'utilisation du disque en format lisible (human-readable)",
                "EASY", false, 50);

        saveMission(ch, 2, "Trouver le coupable",
                "Le disque /var est presque plein. J'ai besoin que tu analyses quels dossiers prennent le plus de place là-dedans. On doit identifier le coupable avant d'agir.",
                "Analyse l'utilisation disque dans /var",
                "[\"du -sh /var/*\", \"du -sh /var/* | sort -rh\", \"du -ah /var | sort -rh | head\"]",
                "du -sh donne la taille de chaque sous-dossier, sort -rh trie du plus grand au plus petit",
                "MEDIUM", false, 75);

        saveMission(ch, 3, "Compresser les logs",
                "C'est les vieux logs le problème. Le fichier /var/log/app.log.old fait 5 gigas à lui seul. Compresse-le pour qu'on gagne de l'espace immédiatement.",
                "Compresse le fichier /var/log/app.log.old",
                "[\"gzip /var/log/app.log.old\", \"bzip2 /var/log/app.log.old\", \"xz /var/log/app.log.old\"]",
                "gzip, bzip2 et xz sont des outils de compression de fichiers",
                "MEDIUM", false, 75);

        // Boss: Le Directeur / Puck (getBossSpeaker — overridden in toDto)
        saveMission(ch, 4, "BOSS — Nettoyage total",
                "C'EST LE DIRECTEUR QUI PARLE. Le serveur va tomber dans les minutes qui viennent, il reste UN POURCENT d'espace ! Trouve et supprime TOUS les fichiers .tmp de plus de 100 mégas sur tout le serveur. C'est notre DERNIÈRE CHANCE, ne me déçois pas !",
                "Trouve et supprime tous les .tmp > 100Mo",
                "[\"find / -name '*.tmp' -size +100M -delete\", \"find / -type f -name '*.tmp' -size +100M -delete\", \"find / -name '*.tmp' -size +100M -exec rm {} \\\\;\", \"find / -type f -name '*.tmp' -size +100M -exec rm {} +\"]",
                "find avec -size +100M cherche les fichiers de plus de 100Mo, -delete ou -exec rm les supprime",
                "HARD", true, 300);
    }

    // ── helper ─────────────────────────────────────────────────────────────────

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
