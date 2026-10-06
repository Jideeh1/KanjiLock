# Third-party notices

KanjiLock includes data, fonts and libraries made by others. The same text is shown in the app under Settings > Sources and licenses (`app/src/main/assets/licenses.txt`).

## JMdict and KANJIDIC2

This application uses the JMdict/EDICT and KANJIDIC dictionary files. These files are the property of the Electronic Dictionary Research and Development Group, and are used in conformance with the Group's licence.

- JMdict: https://www.edrdg.org/wiki/index.php/JMdict-EDICT_Dictionary_Project
- KANJIDIC: https://www.edrdg.org/wiki/index.php/KANJIDIC_Project
- Licence: https://www.edrdg.org/edrdg/licence.html
- License: Creative Commons Attribution-ShareAlike 4.0 International (https://creativecommons.org/licenses/by-sa/4.0/)

Modifications: the data was taken from the JSON releases of jmdict-simplified (https://github.com/scriptin/jmdict-simplified) and condensed into a SQLite database by `tools/build_dict.py`. Only English glosses, the first three senses, up to four alternate spellings and one short example sentence per entry are kept. Entries tagged vulgar, derogatory or sensitive are excluded from the random daily word pool. The resulting database (`app/src/main/assets/dict.db.gz`) is a derived work and is distributed under CC BY-SA 4.0. It is stored unencrypted and can be extracted from the APK.

The data is refreshed from current EDRDG releases before each app release (see "updating the dictionary" in README.md).

## Tatoeba example sentences

Example sentences come from the Tatoeba Project (https://tatoeba.org) and are licensed under CC BY 2.0 FR (https://creativecommons.org/licenses/by/2.0/fr/). Each sentence in the app links to its page on Tatoeba (`https://tatoeba.org/sentences/show/<id>`), where its authors are credited.

## jmdict-simplified

JSON conversion of JMdict and KANJIDIC2 by scriptin and contributors, https://github.com/scriptin/jmdict-simplified. The converted data keeps the CC BY-SA 4.0 license of the source dictionaries.

## Inter font

Copyright 2020 The Inter Project Authors (https://github.com/rsms/inter). Licensed under the SIL Open Font License, Version 1.1 (https://openfontlicense.org). The full license text is included in `app/src/main/assets/licenses.txt`. Bundled as `app/src/main/res/font/inter.ttf`.

## App icon glyph

The 鍵 glyph in the launcher icon is based on the Noto CJK fonts by Google, licensed under the SIL Open Font License 1.1.

## Libraries

| Library | License |
|---|---|
| AndroidX, Jetpack Compose, Material 3 | Apache License 2.0 |
| Google Play services Auth | Android Software Development Kit License |
| zstd-jni (Luben Karavelov) | BSD 2-Clause |
| Zstandard (Meta Platforms, Inc.) | BSD 3-Clause |
| JUnit 4 (tests only) | Eclipse Public License 1.0 |
| sqlite-jdbc (tests only) | Apache License 2.0 |
| org.json (tests only) | Public domain |

## Trademarks

Anki is a registered trademark of Ankitects Pty Ltd. AnkiDroid is an independent open source project. Kotoba is the name of its respective owner's service. Google Drive and Android are trademarks of Google LLC. Samsung, Galaxy and Good Lock are trademarks of Samsung Electronics Co., Ltd. These names are used only to describe compatibility. KanjiLock is not affiliated with, sponsored by or endorsed by any of them.
