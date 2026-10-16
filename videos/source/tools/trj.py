import sys, json, numpy as np
from faster_whisper import WhisperModel
m = WhisperModel("small", device="cpu", compute_type="int8")
for f,o in [(sys.argv[1],sys.argv[2]),(sys.argv[3],sys.argv[4])]:
    a = np.fromfile(f, dtype=np.float32)
    segs,_ = m.transcribe(a, word_timestamps=True, language="en")
    json.dump([{"w":w.word.strip(),"s":round(w.start,2),"e":round(w.end,2)} for s in segs for w in s.words], open(o,"w"))
