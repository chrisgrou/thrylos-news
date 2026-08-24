# Thrylos News

Android εφαρμογή ανάγνωσης άρθρων για τον Ολυμπιακό από πολλαπλές πηγές (RSS
όπου υπάρχει, HTML scraping όπου δεν υπάρχει), με ενιαία e-reader εμφάνιση,
χωρίς διαφημίσεις, offline ανάγνωση, bookmarks, φίλτρα και modular σύστημα
πηγών βασισμένο σε plugin αρχεία JSON — δες [`docs/PLUGIN_FORMAT.md`](docs/PLUGIN_FORMAT.md).

## Αρχιτεκτονική

- **`core:model`** — καθαρά domain types (Kotlin/JVM, χωρίς Android).
- **`core:sources`** — η "μηχανή": plugin parser, RSS/HTML-list/sitemap
  discovery, εξαγωγή άρθρου (Jsoup + αυτόματο Readability fallback), URL
  normalization, keyword/regex φίλτρα, cross-source dedup. Καθαρό Kotlin/JVM,
  καλυμμένο με JUnit5 tests πάνω σε πραγματικά HTML/RSS fixtures.
- **`core:data`** — Room (άρθρα/πηγές/φίλτρα), DataStore (ρυθμίσεις),
  WorkManager (`SyncWorker`/`SyncScheduler`), notifications, backup/OPML.
- **`app`** — Jetpack Compose UI (Material 3): ροή, reader με swipe και 5
  θέματα ανάγνωσης (Light/Sepia/Dark/Black/B&W), ρυθμίσεις πηγών με
  ενσωματωμένο editor+tester, φίλτρα, sync/ειδοποιήσεις, backup.

## Build

```
./gradlew assembleDebug
```

Το APK βγαίνει επίσης αυτόματα σε κάθε push μέσω GitHub Actions
(`.github/workflows/build.yml`) ως artifact.

### Μόνο τη "μηχανή" (χωρίς Android SDK)

```
./gradlew :core:model:test :core:sources:test --configure-on-demand
```

Αυτά τα δύο modules είναι καθαρό Kotlin/JVM και τρέχουν/τεστάρονται χωρίς να
χρειάζονται καθόλου το Android Gradle Plugin ή πρόσβαση στο Google Maven —
χρήσιμο σε περιβάλλοντα με περιορισμένη δικτυακή πρόσβαση.

## Bundled πηγές — χρειάζονται επαλήθευση

Τα 7 plugin JSON κάτω από `app/src/main/assets/plugins/` (to10, athlosnews,
gazzetta, sportal ποδόσφαιρο/μπάσκετ, athletiko, sport24, sport-fm) γράφτηκαν
**χωρίς ζωντανή πρόσβαση** στα ίδια τα sites — το περιβάλλον όπου γράφτηκε ο
κώδικας δεν είχε δικτυακή πρόσβαση σε αυθαίρετα εξωτερικά domains. Είναι
best-effort εικασίες βασισμένες σε συνηθισμένα WordPress patterns
(`/feed/` RSS discovery + γενικοί selectors άρθρου, με αυτόματο Readability
fallback αν αποτύχουν).

**Πριν τα εμπιστευτείς**, άνοιξε κάθε πηγή στις **Ρυθμίσεις → Πηγές →
Επεξεργασία** και πάτησε **«Δοκιμή»** πάνω σε πραγματική συσκευή/δίκτυο. Αν
μια πηγή αποτύχει, διόρθωσε το `discovery.url` (και τα selectors, αν
χρειάζεται) απευθείας στο app — δεν χρειάζεται νέο build.

## Άγνωστα/επόμενα βήματα

- Δεν επαληθεύτηκε τοπικά το compile του `:app`/`:core:data` (χρειάζονται
  Android SDK + πρόσβαση στο Google Maven, μη διαθέσιμα στο περιβάλλον
  ανάπτυξης) — η πρώτη πραγματική επαλήθευση γίνεται στο GitHub Actions CI.
- Δεν υπάρχει drag-and-drop αναδιάταξη πηγών (μόνο πάνω/κάτω βέλη) — μπορεί
  να προστεθεί αργότερα.
- Δεν υπάρχει ακόμα custom γραμματοσειρά για "ευανάγνωστη" επιλογή (mapάρεται
  σήμερα σε sans-serif).
