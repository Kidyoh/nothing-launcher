"""Synthesise SFX + a music bed. usage: python3 sfx.py sfx.json duration out_dir style(1|2)"""
import sys, json, numpy as np
from scipy.signal import butter, sosfilt
from scipy.io import wavfile

SR = 48000
rng = np.random.default_rng(7)
T = lambda d: np.arange(int(d * SR)) / SR

def bp(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, hi], btype='band', fs=SR, output='sos'), x)
def lp(x, f, order=2):
    return sosfilt(butter(order, f, btype='low', fs=SR, output='sos'), x)
def hp(x, f, order=2):
    return sosfilt(butter(order, f, btype='high', fs=SR, output='sos'), x)
def norm(x, peak=.9):
    m = np.max(np.abs(x)) or 1
    return x / m * peak
def env_ad(n, a, d):
    t = np.arange(n) / SR
    return np.where(t < a, t / max(a, 1e-4), np.exp(-(t - a) / d))
def chirp(f0, f1, d, kind='exp'):
    t = T(d)
    f = f0 * (f1 / f0) ** (t / d) if kind == 'exp' else f0 + (f1 - f0) * t / d
    return np.sin(2 * np.pi * np.cumsum(f) / SR)

def sweep_noise(d, f0, f1, q=.6):
    """noise through a band-pass that slides from f0 to f1 (block-wise)."""
    n = int(d * SR); x = rng.standard_normal(n); out = np.zeros(n); blk = 512
    for i in range(0, n, blk):
        p = i / n; fc = f0 * (f1 / f0) ** p
        lo, hi = fc * (1 - q / 2), min(fc * (1 + q / 2), SR / 2 - 100)
        seg = x[max(0, i - 2048):i + blk]
        y = bp(seg, lo, hi)
        out[i:i + blk] = y[-len(out[i:i + blk]):]
    return out

def whoosh(d=.5, up=1800, peak=.42):
    n = int(d * SR); t = np.arange(n) / n
    e = np.sin(np.pi * np.clip(t / peak, 0, 1) * .5) ** 2 * np.where(t < peak, 1, np.exp(-(t - peak) * 7))
    a = sweep_noise(d, 300, up) ; b = sweep_noise(d, 500, up * 1.6)
    return norm((a + .5 * b) * e, .55)
def swipe():
    return norm(whoosh(.28, 4200, .35), .45)
def pop():
    d = .12; s = chirp(1300, 380, d) * env_ad(int(d * SR), .002, .03)
    return norm(s, .55)
def tick():
    d = .05; n = int(d * SR)
    s = np.sin(2 * np.pi * 3800 * T(d)) * env_ad(n, .0005, .008) + .4 * hp(rng.standard_normal(n), 3000) * env_ad(n, .0003, .004)
    return norm(s, .4)
def boom():
    d = .9; n = int(d * SR)
    s = chirp(110, 38, d) * env_ad(n, .003, .28)
    s += .5 * lp(rng.standard_normal(n), 900) * env_ad(n, .001, .05)
    s += .25 * np.sin(2 * np.pi * 55 * T(d)) * env_ad(n, .01, .35)
    return norm(np.tanh(s * 1.6), .8)
def riser():
    d = .55; n = int(d * SR); t = np.arange(n) / n
    s = sweep_noise(d, 400, 7000, .8) * t ** 2 + .25 * chirp(220, 900, d) * t ** 2
    return norm(s, .4)
def scan():
    d = .75; n = int(d * SR); t = T(d)
    s = np.sin(2 * np.pi * (900 + 500 * t / d) * t) * (.5 + .5 * np.sin(2 * np.pi * 22 * t))
    s = .5 * s + .5 * bp(rng.standard_normal(n), 2500, 6000)
    return norm(s * env_ad(n, .05, .5), .28)
def ding():
    d = 1.4; n = int(d * SR); t = T(d)
    s = sum(a * np.sin(2 * np.pi * f * t) * np.exp(-t * k) for f, a, k in [(1568, 1, 3.2), (2349, .55, 4.5), (3136, .3, 6), (4699, .12, 9)])
    return norm(s * env_ad(n, .002, 10), .38)
def strike():
    d = .25; n = int(d * SR)
    s = bp(rng.standard_normal(n), 1500, 5000) * env_ad(n, .01, .07)
    return norm(s, .45)
def stamp():
    b = boom()[:int(.5 * SR)] * .9
    n = len(b); c = bp(rng.standard_normal(n), 800, 4000) * env_ad(n, .0005, .02)
    return norm(b + .7 * c, .85)
def type_():
    d = 1.1; out = np.zeros(int(d * SR)); t = 0
    while t < d - .06:
        k = tick() * rng.uniform(.5, 1); i = int(t * SR); out[i:i + len(k)] += k[:len(out) - i]
        t += rng.uniform(.045, .09)
    return norm(out, .35)
def buzz():
    d = .42; n = int(d * SR); t = T(d)
    s = np.sign(np.sin(2 * np.pi * 98 * t)) + np.sign(np.sin(2 * np.pi * 103.5 * t))
    return norm(lp(s, 1800) * env_ad(n, .005, .25), .45)
def glitch():
    d = .3; n = int(d * SR); s = np.zeros(n)
    for i in range(0, n, 1200):
        f = rng.uniform(200, 3000); s[i:i + 900] = np.sign(np.sin(2 * np.pi * f * T(900 / SR)))[:len(s[i:i + 900])] * rng.uniform(.2, 1)
    return norm(lp(s, 6000) * env_ad(n, .001, .15), .3)
def sparkle():
    d = .9; out = np.zeros(int(d * SR))
    for j, f in enumerate([2093, 2637, 3136, 4186, 3520]):
        i = int(j * .07 * SR); nn = len(out) - i; tt = np.arange(nn) / SR
        out[i:] += np.sin(2 * np.pi * f * tt) * np.exp(-tt * 9)
    return norm(out, .3)

LIB = dict(whoosh=whoosh, swipe=swipe, pop=pop, tick=tick, boom=boom, riser=riser, scan=scan, ding=ding,
           strike=strike, stamp=stamp, type=type_, buzz=buzz, glitch=glitch, sparkle=sparkle)

def music(dur, style):
    """soft lo-fi bed: chords + kick + hats. style 1 = 96bpm A minor, style 2 = 104bpm F major."""
    bpm, prog = (96, [[57, 60, 64, 67], [53, 57, 60, 64], [48, 52, 55, 59], [55, 59, 62, 65]]) if style == 1 else \
                (104, [[53, 57, 60, 64], [50, 53, 57, 60], [46, 50, 53, 57], [48, 52, 55, 58]])
    beat = 60 / bpm; bar = 4 * beat; n = int((dur + 2) * SR); out = np.zeros(n)
    m2f = lambda m: 440 * 2 ** ((m - 69) / 12)
    b = 0; t0 = 0.0
    while t0 < dur + 1:
        ch = prog[b % 4]; i = int(t0 * SR); L = min(int(bar * SR), n - i); t = np.arange(L) / SR
        e = np.minimum(t / .25, 1) * np.exp(-t * .45)
        for m in ch:
            f = m2f(m); out[i:i + L] += .12 * e * np.sin(2 * np.pi * f * t + 1.2 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t * 3))
        f = m2f(ch[0] - 24); out[i:i + L] += .22 * np.minimum(t / .02, 1) * np.exp(-t * .9) * np.sin(2 * np.pi * f * t)
        for k in range(4):
            j = i + int(k * beat * SR)
            if k in (0, 2):  # kick
                kd = int(.35 * SR); kt = np.arange(kd) / SR
                kk = .55 * np.sin(2 * np.pi * np.cumsum(50 + 90 * np.exp(-kt * 30)) / SR) * np.exp(-kt * 9)
                out[j:j + kd] += kk[:max(0, min(kd, n - j))]
            if k in (1, 3):  # soft snap
                sd = int(.18 * SR); sn = .18 * bp(rng.standard_normal(sd), 1200, 5000) * np.exp(-np.arange(sd) / SR * 28)
                out[j:j + sd] += sn[:max(0, min(sd, n - j))]
            for h in (0, .5):  # hats
                jj = j + int(h * beat * SR); hd = int(.05 * SR)
                hh = .06 * hp(rng.standard_normal(hd), 7000) * np.exp(-np.arange(hd) / SR * 80)
                out[jj:jj + hd] += hh[:max(0, min(hd, n - jj))]
        b += 1; t0 += bar
    out = lp(out, 5000)
    out = out[:int(dur * SR)]
    fade = int(1.2 * SR); out[-fade:] *= np.linspace(1, 0, fade); out[:int(.3 * SR)] *= np.linspace(0, 1, int(.3 * SR))
    return norm(out, .5)

if __name__ == '__main__':
    events = json.load(open(sys.argv[1])); dur = float(sys.argv[2]); od = sys.argv[3]; style = int(sys.argv[4])
    tr = np.zeros(int((dur + 2) * SR)); cache = {}
    for t, name, g in events:
        if name not in cache: cache[name] = LIB[name]()
        s = cache[name] * g; i = int(t * SR); L = min(len(s), len(tr) - i); tr[i:i + L] += s[:L]
    tr = tr[:int(dur * SR)]
    tr = np.clip(tr, -.98, .98)
    wavfile.write(f'{od}/sfx.wav', SR, (tr * 32767).astype(np.int16))
    wavfile.write(f'{od}/music.wav', SR, (music(dur, style) * 32767).astype(np.int16))
    print('ok', len(events), 'events')
