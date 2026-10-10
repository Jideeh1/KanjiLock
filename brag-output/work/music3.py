import numpy as np, wave, sys
from scipy.signal import fftconvolve, butter, sosfilt

SR = 44100
BPM = 128
B = 60 / BPM
TOTAL_BEATS = 52
DUR = TOTAL_BEATS * B + 1.6
N = int(DUR * SR)
rng = np.random.default_rng(7)
W = sys.argv[1]

def mtof(m): return 440 * 2 ** ((m - 69) / 12)
def env(n, a=0.005, d=0.2, s=0.0, r=0.05, hold=None):
    t = np.arange(n) / SR
    e = np.minimum(1, t / max(a, 1e-4))
    dec = s + (1 - s) * np.exp(-(t - a) / d)
    e = np.where(t < a, e, dec)
    if hold is not None:
        e = e * np.clip(1 - (t - hold) / r, 0, 1)
    return e
def lp(x, f, order=2):
    return sosfilt(butter(order, f / (SR / 2), 'low', output='sos'), x)
def hp(x, f, order=2):
    return sosfilt(butter(order, f / (SR / 2), 'high', output='sos'), x)
def add(buf, x, t, g=1.0):
    i = int(t * SR)
    if i >= len(buf): return
    x = x[: len(buf) - i]
    buf[i:i + len(x)] += x * g
def saw(f, n, det=(0,)):
    t = np.arange(n) / SR
    out = 0
    for d in det:
        ff = f * 2 ** (d / 1200)
        out = out + 2 * ((t * ff + rng.random()) % 1) - 1
    return out / len(det)

drums = np.zeros(N); bass = np.zeros(N); keys = np.zeros(N); lead = np.zeros(N); fx = np.zeros(N); sfx = np.zeros(N)

def kick():
    n = int(0.45 * SR); t = np.arange(n) / SR
    f = 48 + 110 * np.exp(-t * 28)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * np.exp(-t * 7) + 0.12 * lp(rng.standard_normal(n), 2000) * np.exp(-t * 120)
def clap():
    n = int(0.3 * SR); t = np.arange(n) / SR
    nz = rng.standard_normal(n)
    e = np.exp(-t * 22) + 0.6 * np.exp(-np.maximum(0, t - 0.012) * 60) * (t > 0.012)
    return hp(lp(nz, 6000), 900) * e * 0.6
def hat(open_=False):
    n = int((0.25 if open_ else 0.06) * SR); t = np.arange(n) / SR
    return hp(rng.standard_normal(n), 7000, 4) * np.exp(-t * (14 if open_ else 70))

K, C, H, HO = kick(), clap(), hat(), hat(True)
prog = [(57, [57, 60, 64]), (53, [53, 57, 60]), (48, [55, 60, 64]), (55, [55, 59, 62])]
pent = [69, 72, 74, 76, 79, 81, 84]


END = 46
def stab(chord, t, g, cutoff=2400, dur=0.28):
    n = int(dur * SR)
    x = sum(lp(saw(mtof(m), n, (-9, 9)), cutoff) for m in chord) / len(chord)
    add(keys, x * env(n, 0.003, 0.1), t, g)
def impact(t, g=0.7):
    n = int(1.0 * SR); tt = np.arange(n) / SR
    x = np.sin(2 * np.pi * (38 + 70 * np.exp(-tt * 9)) * tt) * np.exp(-tt * 3.5) + 0.35 * lp(rng.standard_normal(n), 1800) * np.exp(-tt * 8)
    add(fx, x, t, g)
def whoosh(t_cut, g=0.09, length=0.32):
    n = int(length * SR); tt = np.arange(n) / SR
    nz = rng.standard_normal(n); out = np.zeros(n)
    for i, ch in enumerate(np.array_split(np.arange(n), 16)):
        out[ch] = lp(nz, 900 + 6000 * (i / 16))[ch]
    add(fx, hp(out, 400) * np.sin(np.pi * tt / tt[-1]) ** 2, t_cut - length * 0.7, g)
def riser(t0, t1, g=0.2):
    n = int((t1 - t0) * SR); tt = np.arange(n) / SR; nz = rng.standard_normal(n); out = np.zeros(n)
    for i, ch in enumerate(np.array_split(np.arange(n), 40)):
        out[ch] = lp(nz, 300 + 9000 * (i / 40) ** 2)[ch]
    pr = tt / tt[-1]
    tone = np.sin(2 * np.pi * np.cumsum(220 + 660 * pr ** 2) / SR) * 0.15
    add(fx, (out + tone) * pr ** 2, t0, g)

# --- hook (beats 0-4): stab + kick on every 8th, rising
for i in range(8):
    t = i * B / 2
    root, chord = prog[[0, 0, 1, 1, 2, 2, 3, 3][i]]
    stab([c + 12 for c in chord], t, 0.5, 1800 + 300 * i)
    add(drums, K, t, 0.55 if i % 2 == 0 else 0.3)
    add(drums, C, t, 0.08 + 0.03 * i)
    add(bass, np.sin(2 * np.pi * mtof(root - 24) * np.arange(int(0.2 * SR)) / SR) * env(int(0.2 * SR), 0.003, 0.12), t, 0.5)
riser(2 * B, 4 * B, 0.12)

# --- groove 4..46
for beat in range(4, END):
    t = beat * B; bar = beat // 4
    root, chord = prog[bar % 4]
    build = 10 <= beat < 12
    add(drums, K, t, 0.95 if not build else 0.6)
    if beat % 2 == 1: add(drums, C, t, 0.42)
    for s in range(4):
        if s % 2: add(drums, H, t + s * B / 4, 0.08 if s == 1 else 0.05)
    add(drums, HO, t + B / 2, 0.06)
    if build:
        for s in range(4): add(drums, C, t + s * B / 4, 0.1 + 0.08 * (beat - 10) + 0.03 * s)
    for s in (1, 3):
        n = int(B / 2 * SR * 0.9); f = mtof(root - 24); tt = np.arange(n) / SR
        x = 0.7 * np.sin(2 * np.pi * f * tt) + 0.35 * lp(saw(f, n, (0, 7)), 900)
        add(bass, x * env(n, 0.004, 0.18, 0.5, 0.03, hold=n / SR - 0.03), t + s * B / 4, 0.55)
    for off in ((0.5, 1.5) if beat % 2 == 0 else (0.75,)):
        stab(chord, t + off * B, 0.4, 1700 if beat < 12 else 2300)
    if beat >= 12:
        for s in range(4):
            if (beat * 4 + s) % 3 == 0 or s == 2:
                m_ = pent[(beat * 3 + s * 2) % len(pent)]
                if m_ % 12 not in [c % 12 for c in chord] + [(chord[0] + 2) % 12, (chord[0] + 7) % 12]:
                    m_ = chord[(s + beat) % 3] + 12
                n = int(0.2 * SR); tt = np.arange(n) / SR
                x = np.sin(2 * np.pi * mtof(m_) * tt) + 0.3 * np.sin(2 * np.pi * mtof(m_) * 3 * tt) * np.exp(-tt * 30)
                add(lead, x * env(n, 0.002, 0.07), t + s * B / 4, 0.15)
    if 39 <= beat < 42:
        for s in range(4):
            n = int(0.12 * SR); tt = np.arange(n) / SR
            add(lead, np.sin(2 * np.pi * mtof(pent[(beat * 4 + s) % 7] + 12) * tt) * env(n, 0.002, 0.05), t + s * B / 4, 0.12)
riser(8 * B, 12 * B, 0.22)
riser(44 * B, 46 * B, 0.14)

# --- outro 46..52
root, chord = prog[0]
n = int(4.5 * SR); tt = np.arange(n) / SR
fin = sum(lp(saw(mtof(m_), n, (-10, 0, 10)), 3000) for m_ in [45, 57, 60, 64, 69, 76]) / 6
add(keys, fin * np.exp(-tt * 0.9) * np.minimum(1, tt / 0.01), END * B, 0.85)
add(drums, K, END * B, 1.0)
for beat in range(END, 51):
    for s in range(4):
        if s % 2: add(drums, H, beat * B + s * B / 4, 0.035)
for i, m_ in enumerate([81, 84, 88, 93]):
    n = int(1.5 * SR); tt = np.arange(n) / SR
    add(lead, np.sin(2 * np.pi * mtof(m_) * tt) * np.exp(-tt * 2.2), END * B + 0.2 + i * B / 2, 0.11)

for h in [4, 12, 16, 20, 24, 28, 32, 35, 39, 42, 46]:
    impact(h * B, 0.75 if h in (4, 12, 46) else 0.4)
for c in [8, 12, 16, 20, 24, 28, 32, 35, 39, 42, 46]:
    whoosh(c * B, 0.11)
for h in [25, 26, 27, 43, 44, 45]:
    impact(h * B, 0.22)

def load(name):
    w = wave.open(f'{W}/{name}.wav'); x = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(float) / 32768
    if w.getnchannels() == 2: x = x.reshape(-1, 2).mean(1)
    return x / (np.abs(x).max() + 1e-9)
good, easy, ach = load('sfx_good'), load('sfx_easy'), load('sfx_achievement')
r = 2 ** (-4 / 12)
ach = np.interp(np.arange(0, len(ach) - 1, r), np.arange(len(ach)), ach)
add(sfx, good, 5 * B + 1.5, 0.30)
add(sfx, easy, 11 * B + 1.15, 0.18)
add(sfx, good, 11 * B + 2.05, 0.28)
add(sfx, good, 17 * B + 2.35, 0.28)
add(sfx, ach, 24 * B + 1.05, 0.30)
add(sfx, easy, 35 * B + 1.35, 0.22)


add(sfx, good, 10 * B, 0.30)
add(sfx, easy, 17.5 * B, 0.2)
add(sfx, good, 18.5 * B, 0.28)
add(sfx, good, 23 * B + 0.2, 0.28)
add(sfx, ach, 27.5 * B, 0.30)
add(sfx, easy, 34 * B, 0.24)

side = np.ones(N)
for beat in range(4, END + 1):
    i = int(beat * B * SR); n = int(B * SR)
    tt = np.arange(n) / SR
    side[i:i + n] = np.minimum(side[i:i + n][:len(tt)], 1 - 0.55 * np.exp(-tt * 9)[:len(side[i:i+n])])
irn = int(1.6 * SR); ir = rng.standard_normal(irn) * np.exp(-np.arange(irn) / SR * 3.2)
ir = lp(ir, 5000); ir /= np.sqrt((ir ** 2).sum())
def verb(x, wet): return x + wet * fftconvolve(x, ir)[:N]

mix = drums * 0.9 + bass * side * 0.8 + verb(keys * side, 0.35) * 0.75 + verb(lead, 0.45) * 0.7 + verb(fx, 0.3) + verb(lp(sfx, 9000), 0.3)
mix = lp(hp(mix, 30), 11000)
mix = np.tanh(mix * 1.1) / np.tanh(1.1)
fade = int(0.8 * SR); mix[-fade:] *= np.linspace(1, 0, fade)
mix = mix / np.abs(mix).max() * 0.89
st = np.stack([mix, mix], 1)
dl = int(0.012 * SR); st[dl:, 1] = 0.85 * mix[:-dl] + 0.15 * mix[dl:]
out = (st * 32767).astype(np.int16)
w = wave.open(f'{W}/music.wav', 'wb'); w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(out.tobytes()); w.close()
print('ok', DUR)
