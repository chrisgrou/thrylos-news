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
