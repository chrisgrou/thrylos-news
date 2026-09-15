# Μορφή πηγής (plugin JSON)

Κάθε πηγή άρθρων είναι ένα αρχείο JSON. Η εφαρμογή δεν χρειάζεται update για να
προσθέσεις ή να διορθώσεις μια πηγή — απλώς εισάγεις (ή επεξεργάζεσαι) το JSON
της από **Ρυθμίσεις → Πηγές**, και δοκιμάζεις με το κουμπί **«Δοκιμή»**.

## Πλήρες παράδειγμα

```json
{
  "schemaVersion": 1,
  "id": "gazzetta-olympiacos",
  "name": "Gazzetta — Ολυμπιακός",
  "homepage": "https://www.gazzetta.gr",
  "enabled": true,
  "discovery": {
    "type": "rss",
    "url": "https://www.gazzetta.gr/rss/team/olympiacos",
    "maxItems": 40
  },
  "listSelectors": {
    "item": "article.teaser",
    "link": "a@href",
    "title": "h3",
    "image": "img@src",
    "date": "time@datetime"
  },
  "article": {
    "title": "h1.article-title",
    "author": ".author-name",
    "date": "time.published@datetime",
    "dateFormat": "yyyy-MM-dd'T'HH:mm:ssXXX",
    "leadImage": "figure.lead img@src",
    "content": "div.article-body",
    "remove": [".ad", ".adsbygoogle", "iframe", "script", "style", ".related", ".newsletter"],
    "unwrap": ["div", "span"]
  },
  "urlRules": {
    "allow": ["^https://www\\.gazzetta\\.gr/football/olympiacos/"],
    "deny": ["/live/", "/photos/"],
    "stripQueryParams": ["utm_source", "utm_medium", "fbclid"]
  },
  "http": { "userAgent": "default", "headers": {}, "delayMs": 300 },
  "fallback": "readability"
}
```

## Κανάλι YouTube ως πηγή

Ο πιο εύκολος τρόπος: **Ρυθμίσεις → Πηγές → Νέα πηγή → Κανάλι YouTube**, βάλε
τον σύνδεσμο ή το `@handle` του καναλιού, και η εφαρμογή βρίσκει μόνη της το
channel ID και το πραγματικό όνομα (δεν χρειάζεται να τα ψάξεις χειροκίνητα).
Αν αυτό αποτύχει για κάποιο κανάλι, υπάρχει επιλογή «Χειροκίνητα (JSON)» που
ανοίγει τον ίδιο επεξεργαστή JSON με ό,τι κι άλλη πηγή — τα παρακάτω εξηγούν
τι περιέχει αυτό το JSON.

`"kind": "youtube"` δηλώνει ότι η πηγή είναι ένα κανάλι YouTube αντί για site.
Δεν κατεβάζει/σκράπει καμία σελίδα — ένα video δεν έχει άρθρο-σελίδα να
διαβαστεί (JS εφαρμογή, όχι στατικό HTML), οπότε το "άρθρο" χτίζεται απευθείας
από τα δεδομένα του ίδιου του feed του καναλιού (τίτλος, thumbnail,
περιγραφή, ημερομηνία). Γι' αυτό `discovery.type` είναι πάντα `"rss"`,
δείχνοντας στο Atom feed του καναλιού:

```json
{
  "schemaVersion": 1,
  "id": "rednews-youtube",
  "name": "RedNews",
  "homepage": "https://www.youtube.com/@REDSPORTS7",
  "kind": "youtube",
  "discovery": {
    "type": "rss",
    "url": "https://www.youtube.com/feeds/videos.xml?channel_id=UCGiTb1kleEoNRKPPhwBUDCg",
    "maxItems": 30
  },
  "article": { "title": "unused for kind=youtube" }
}
```

`discovery.url` χρειάζεται το **channel ID** (ξεκινά με `UC...`), όχι το
`@handle` — το YouTube δεν προσφέρει RSS feed με βάση το handle. Βρίσκεται
από το κανάλι → «Σχετικά» → «Κοινοποίηση καναλιού» → «Αντιγραφή
αναγνωριστικού καναλιού». Το `name` είναι το πραγματικό όνομα του καναλιού
(π.χ. "RedNews"), ώστε αυτό να φαίνεται ως πηγή στην εφαρμογή — όχι το
handle. Το `article.content` είναι προαιρετικό (παραλείπεται εντελώς, όπως
στο παράδειγμα) — μόνο για `kind: "youtube"`, αφού δεν υπάρχει σελίδα να
διαβαστεί. Το `article.title` παραμένει υποχρεωτικό πεδίο του schema αλλά
δεν χρησιμοποιείται ποτέ σε αυτή την περίπτωση (οι τίτλοι των video έρχονται
από το feed) — βάλε οποιαδήποτε τιμή.

`discovery.excludeShorts` (προαιρετικό, `false` από προεπιλογή) αποκλείει τα
Shorts του καναλιού από τη λίστα — το feed τα αναμειγνύει με τα κανονικά
video, με URL της μορφής `/shorts/ID` αντί για `watch?v=ID`. Ελέγχεται από
το toggle «Εμφάνιση Shorts» τόσο στην προσθήκη όσο και στην επεξεργασία μιας
πηγής `kind: "youtube"` — δεν χρειάζεται να το πειράξεις εδώ με το χέρι, εκτός
αν επεξεργάζεσαι το JSON απευθείας.

## Πεδία

| Πεδίο | Υποχρεωτικό | Περιγραφή |
|---|---|---|
| `schemaVersion` | ✔ | Πάντα `1` στην τρέχουσα έκδοση. |
| `id` | ✔ | Μοναδικό, λατινικά πεζά/ψηφία/παύλες, π.χ. `gazzetta-olympiacos`. |
| `name` | ✔ | Όνομα που βλέπει ο χρήστης. |
| `homepage` | ✔ | Μόνο για εμφάνιση/OPML. |
| `discovery.type` | ✔ | `rss`, `html-list`, ή `sitemap`. |
| `discovery.url` | ✔ | Το feed URL, η σελίδα λίστας, ή το sitemap. |
| `listSelectors` | μόνο για `html-list` | Πώς εξάγεται κάθε άρθρο από τη σελίδα λίστας. |
| `article.title` / `article.content` | ✔ | Selectors για τίτλο και κυρίως κείμενο του άρθρου. |
| `article.remove` | — | Επιπλέον selectors για αφαίρεση (πέρα από την ενσωματωμένη ad-blocklist). |
| `urlRules` | — | Φιλτράρισμα/καθαρισμός URLs πριν την άντληση. |
| `fallback` | — | `readability` (προεπιλογή) ή `none`. |

## Selector syntax

Κάθε selector είναι ένα Jsoup CSS selector, με προαιρετικό `@attr` στο τέλος
για να διαβαστεί ένα attribute αντί για το κείμενο, π.χ.:

- `h1.article-title` → κείμενο του πρώτου `h1.article-title`
- `img@src` → η τιμή του `src`
- `time.published@datetime` → η τιμή του `datetime`

## Fallback εξαγωγής

Αν οι selectors του `article` αποτύχουν ή επιστρέψουν πολύ λίγο κείμενο, η
εφαρμογή δοκιμάζει αυτόματα [Readability](https://github.com/mozilla/readability)
πάνω στη σελίδα — έτσι ένα ατελές plugin εξακολουθεί να δίνει διαβάσιμο άρθρο.

## Tips για γρήγορη προσθήκη πηγής

1. Δοκίμασε πρώτα `discovery.type: "rss"` με URL `<site>/feed/` ή `<κατηγορία>/feed/`
   — οι περισσότερες ελληνικές σελίδες (WordPress) το υποστηρίζουν.
2. Αν δεν υπάρχει RSS, χρησιμοποίησε `html-list` και βρες τα selectors μέσω
   "Προβολή πηγαίου κώδικα" στον browser του κινητού ή του υπολογιστή σου.
3. Χρησιμοποίησε πάντα **«Δοκιμή»** πριν αποθηκεύσεις — δείχνει live τι
   εξήχθη (τίτλος, συντάκτης, αριθμός λέξεων) πάνω σε πραγματικό άρθρο.
