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

## Κατάσταση bundled πηγών

| Πηγή | Κατάσταση |
|---|---|
| **to10** | ✅ Επαληθευμένο σε πραγματικό HTML (`html-list` — το site δεν έχει RSS), με tests |
| **sport-fm** | ✅ Εξαγωγή άρθρου επαληθευμένη· discovery μη επιβεβαιωμένο (βρέθηκε πραγματικό Atom feed στο `/tag/olympiakos.feed`, αλλά δεν είχαμε snapshot του ίδιου του feed για test) |
| **sport24** | ✅ Selectors επαληθευμένοι σε πραγματικό HTML (list + article)· το 403 στη συσκευή χρειάζεται δοκιμή με το νέο desktop User-Agent που μπήκε στο plugin — άγνωστο αν αρκεί |
| **athlosnews** | ✅ Πραγματικό RSS feed βρέθηκε (`/category/omada/olympiakos/feed/` — το αρχικό slug `big-5` ήταν λάθος), εξαγωγή άρθρου επαληθευμένη |
| **gazzetta** | ✅ Επαληθευμένο σε πραγματικό HTML (`html-list` — το site δεν έχει RSS καθόλου) |
| **athletiko** | ✅ Επαληθευμένο σε πραγματικό HTML (`html-list` — το site δεν έχει per-team RSS) |
| **sportal (ποδόσφαιρο/μπάσκετ)** | ✅ Επαληθευμένο σε πραγματικό HTML (`html-list`)· διορθώθηκε λάθος typo στο URL του μπάσκετ (διπλό "b" αντί για ένα στο slug) |
| **sportdog** | ✅ Επαληθευμένο σε πραγματικό HTML (`html-list` — το site δεν έχει RSS) |

Οι επαληθευμένες πηγές τρέχουν πάνω σε πραγματικά snapshots από
`core/sources/src/test/resources/fixtures/` μέσω `To10PluginTest`,
`SportFmPluginTest`, `Sport24PluginTest`, `AthlosnewsPluginTest`,
`GazzettaPluginTest`, `AthletikoPluginTest`, `SportalPluginTest`,
`SportdogPluginTest` — τα tests φορτώνουν τα **shipped** plugin JSON από το
`app/src/main/assets/plugins/`, ώστε plugin και tests να μη μπορούν να
αποκλίνουν.

Οι υπόλοιπες πηγές είναι ακόμα best-effort και **δεν δουλεύουν**. Για να
διορθωθεί μια πηγή χρειάζονται δύο page snapshots (Ctrl+S → «Web Page, single
file» ή View-Source): μία σελίδα λίστας άρθρων και ένα άρθρο. Μετά τη
διόρθωση, μπορείς να επεξεργαστείς οποιοδήποτε plugin και από τις **Ρυθμίσεις →
Πηγές → Επεξεργασία → «Δοκιμή»** χωρίς νέο build.

## Άγνωστα/επόμενα βήματα

- Δεν επαληθεύτηκε τοπικά το compile του `:app`/`:core:data` (χρειάζονται
  Android SDK + πρόσβαση στο Google Maven, μη διαθέσιμα στο περιβάλλον
  ανάπτυξης) — η πρώτη πραγματική επαλήθευση γίνεται στο GitHub Actions CI.
- Δεν υπάρχει drag-and-drop αναδιάταξη πηγών (μόνο πάνω/κάτω βέλη) — μπορεί
  να προστεθεί αργότερα.
- Δεν υπάρχει ακόμα custom γραμματοσειρά για "ευανάγνωστη" επιλογή (mapάρεται
  σήμερα σε sans-serif).
