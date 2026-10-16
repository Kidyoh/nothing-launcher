#!/bin/bash
# assemble.sh <n> <src.mp4> <dur> <style> <out.mp4>
set -e
S=/tmp/claude-0/-home-user-nothing-launcher/29108185-9574-5d4f-abd6-4f260ff18759/scratchpad
n=$1; src=$2; dur=$3; style=$4; out=$5; O=$S/out$n
python3 -I $S/tools/sfx.py $O/sfx.json $dur $O $style
ffmpeg -v error -y -framerate 30 -i $O/%05d.jpg -i "$src" -i $O/sfx.wav -i $O/music.wav -filter_complex "
[1:a]aresample=48000,highpass=f=85,afftdn=nf=-28,acompressor=threshold=-20dB:ratio=3:attack=5:release=80,equalizer=f=3500:t=q:w=1.2:g=2.5,loudnorm=I=-15:TP=-1.5:LRA=9,asplit=2[v][vsc];
[3:a]volume=0.22[m0];[m0][vsc]sidechaincompress=threshold=0.03:ratio=6:attack=20:release=350[m];
[2:a]volume=0.75[s];
[v][s][m]amix=inputs=3:normalize=0,alimiter=limit=0.95[a]" \
 -map 0:v -map "[a]" -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p -profile:v high -movflags +faststart -c:a aac -b:a 192k -t $dur "$out"
ffprobe -v error -show_entries format=duration:stream=width,height -of compact "$out"
