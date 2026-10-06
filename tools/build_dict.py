#run it next to the jmdict-simplified and kanjidic2 json files, makes dict.db
#then gzip -9 it into app/src/main/assets
import json, glob, sqlite3, os, re
J=json.load(open(glob.glob("jmdict-examples-eng*.json")[0]))
K=json.load(open(glob.glob("kanjidic2-en*.json")[0]))
out="dict.db"
if os.path.exists(out): os.remove(out)
db=sqlite3.connect(out)
db.executescript("""
PRAGMA page_size=4096;
CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT);
CREATE TABLE words(id INTEGER PRIMARY KEY, word TEXT NOT NULL, reading TEXT NOT NULL, alt TEXT, meaning TEXT NOT NULL,
  pos TEXT, common INTEGER NOT NULL, kana_only INTEGER NOT NULL, ex_jp TEXT, ex_en TEXT, ex_id TEXT);
CREATE TABLE kanji(literal TEXT PRIMARY KEY, reading TEXT NOT NULL, onyomi TEXT, kunyomi TEXT, meaning TEXT NOT NULL,
  grade INTEGER, jlpt INTEGER, strokes INTEGER, freq INTEGER, ex_word TEXT, ex_reading TEXT, ex_meaning TEXT);
""")
def kana_ok(s): return all('぀'<=c<='ヿ' or c in 'ー・' for c in s)
rows=[]
for w in J["words"]:
    kanji=[k for k in w["kanji"] if not any(t in ("sK","iK","oK","rK","ateji") for t in k["tags"])] or w["kanji"]  #skip search only n irregular spellings
    kana=[k for k in w["kana"] if not any(t in ("sk","ik","ok","rk") for t in k["tags"])] or w["kana"]
    if not kana: continue
    kanji_c=[k for k in kanji if k.get("common")] or kanji
    kana_c=[k for k in kana if k.get("common")] or kana
    usually_kana = bool(w["sense"]) and "uk" in (w["sense"][0].get("misc") or [])  # uk means usually kana so show the kana
    primary = kanji_c[0]["text"] if kanji_c and not usually_kana else kana_c[0]["text"]
    reading = kana_c[0]["text"]
    if kanji_c and not usually_kana:
        apt=[k for k in kana if "*" in k.get("appliesToKanji",["*"]) or primary in k.get("appliesToKanji",[])]
        apt=[k for k in apt if k.get("common")] or apt
        if apt: reading=apt[0]["text"]
    alts=[k["text"] for k in kanji+kana if k["text"] not in (primary,reading)][:4]
    kana_only_flag = 1 if (not w["kanji"] or usually_kana) else 0
    senses=[s for s in w["sense"] if any(g["lang"]=="eng" for g in s["gloss"])]
    if not senses: continue
    parts=[]
    for s in senses[:3]:
        gl=[g["text"] for g in s["gloss"] if g["lang"]=="eng"][:3]
        if gl: parts.append(", ".join(gl))
    meaning="; ".join(parts)
    pos=",".join(senses[0].get("partOfSpeech",[])[:2])
    common=1 if any(k.get("common") for k in w["kanji"]+w["kana"]) else 0
    if any(m in ("vulg","derog","sens","X") for sn in w["sense"] for m in (sn.get("misc") or [])): common=0  #keep the bad words out of the daily pool, still searchable tho
    ex_jp=ex_en=ex_id=None
    exs=[]
    for sn in w["sense"]:
        for e in sn.get("examples") or []:
            jp=[x["text"] for x in e["sentences"] if x["lang"]=="jpn"]; en=[x["text"] for x in e["sentences"] if x["lang"]=="eng"]
            src=e.get("source") or {}
            sid=src.get("value") if src.get("type")=="tatoeba" else None
            if jp and en and len(jp[0])<=40: exs.append((len(jp[0]),jp[0],en[0],sid))
    #shortest sentence wins, long ones dont fit the widget
    if exs:
        exs.sort(key=lambda t:(t[0],t[1])); ex_jp,ex_en,ex_id=exs[0][1],exs[0][2],exs[0][3]
    rows.append((int(w["id"]),primary,reading,";".join(alts) or None,meaning,pos,common,kana_only_flag,ex_jp,ex_en,ex_id))
db.executemany("INSERT OR IGNORE INTO words VALUES (?,?,?,?,?,?,?,?,?,?,?)",rows)
print("words", len(rows))

#example word for each kanji, prefer words where the other kanji are common
common_words=[r for r in rows if r[6]==1 and r[7]==0]
freq={c["literal"]:(c["misc"].get("frequency") or 3000) for c in K["characters"]}
by_char={}
for r in common_words:
    for ch in set(r[1]):
        if '一'<=ch<='鿿': by_char.setdefault(ch,[]).append(r)
def kata2hira(s): return "".join(chr(ord(c)-0x60) if 'ァ'<=c<='ヶ' else c for c in s)
krows=[]
for c in K["characters"]:
    m=c["misc"]; lit=c["literal"]
    if not (m.get("grade") or m.get("jlptLevel") or (m.get("frequency") or 9999)<=2500): continue
    groups=(c.get("readingMeaning") or {}).get("groups",[])
    on=[r["value"] for g in groups for r in g["readings"] if r["type"]=="ja_on"]
    kun=[r["value"] for g in groups for r in g["readings"] if r["type"]=="ja_kun"]
    means=[x["value"] for g in groups for x in g["meanings"] if x["lang"]=="en"]
    if not means or not (on or kun): continue
    plain_kun=[k for k in kun if "." not in k and "-" not in k]
    standalone=sorted([r for r in common_words if r[1]==lit], key=lambda r: (0 if r[2] in plain_kun else 1, r[0]))
    #if the kanji is a word by itself use that reading, 本 should be ほん not もと
    reading = standalone[0][2] if standalone else plain_kun[0] if plain_kun and not on else on[0] if on else (kun[0].replace("-","").replace(".","") if kun and not on else on[0] if on else kun[0].replace(".","").replace("-",""))
    cands=[r for r in by_char.get(lit,[]) if r[1]!=lit and 2<=len(r[1])<=4
           and all(('\u4e00'<=ch<='\u9fff') or ('\u3040'<=ch<='\u309f') for ch in r[1])]
    def score(r):
        others=[freq.get(ch,3000) for ch in r[1] if '\u4e00'<=ch<='\u9fff' and ch!=lit]
        return (max(others) if others else 0, len(r[1]))
    cands.sort(key=score)
    ex=cands[0] if cands else None
    krows.append((lit,reading,"、".join(on[:4]) or None,"、".join(kun[:5]) or None,", ".join(means[:4]),m.get("grade"),m.get("jlptLevel"),(m.get("strokeCounts") or [None])[0],m.get("frequency"),
                  ex[1] if ex else None, ex[2] if ex else None, ex[4].split(";")[0] if ex else None))
db.executemany("INSERT INTO kanji VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",krows)
print("kanji", len(krows))
db.executescript("""
CREATE INDEX words_word ON words(word);
CREATE INDEX words_reading ON words(reading);
CREATE INDEX words_pool ON words(common, kana_only);
CREATE INDEX kanji_grade ON kanji(grade);
""")
db.executemany("INSERT INTO meta VALUES (?,?)",[("jmdict_date",J["dictDate"]),("kanjidic_date",K["dictDate"]),("version","2"),
 ("credits","JMdict and KANJIDIC2 © Electronic Dictionary Research and Development Group (EDRDG), CC BY-SA 4.0. Example sentences from Tatoeba, CC BY 2.0 FR.")])
db.commit(); db.execute("VACUUM"); db.close()
print("size MB", round(os.path.getsize(out)/1e6,1))
