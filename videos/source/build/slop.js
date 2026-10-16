// Stereotypical "AI slop" app screens, used as the "before" examples.
const SLOP={
1:`<div style="position:absolute;inset:0;background:linear-gradient(165deg,#5b21b6 0%,#9333ea 45%,#ec4899 100%);color:#fff;font-family:Inter;overflow:hidden">
  <div style="position:absolute;width:260px;height:260px;border-radius:50%;background:#f0abfc;filter:blur(60px);opacity:.55;left:-80px;top:40px"></div>
  <div style="position:absolute;width:220px;height:220px;border-radius:50%;background:#22d3ee;filter:blur(60px);opacity:.45;right:-70px;top:360px"></div>
  <div style="position:relative;padding:70px 26px 0">
    <div style="display:inline-block;padding:6px 12px;border-radius:99px;background:rgba(255,255,255,.18);font-size:12px;font-weight:600">✨ Powered by AI</div>
    <div style="font-size:38px;font-weight:800;line-height:1.05;margin-top:18px;letter-spacing:-.02em">Unlock Your<br><span style="background:linear-gradient(90deg,#fde68a,#f9a8d4);-webkit-background-clip:text;color:transparent">Full Potential</span> 🚀</div>
    <div style="font-size:14px;opacity:.85;margin-top:14px;line-height:1.4">Revolutionary AI-powered solutions to supercharge your workflow and transform the future.</div>
    <div style="margin-top:26px;padding:15px;border-radius:16px;text-align:center;font-weight:700;background:linear-gradient(90deg,#a855f7,#ec4899);box-shadow:0 0 40px #e879f9,0 0 80px #a855f7">Get Started ✨</div>
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:26px">
      ${['⚡ Fast','🔒 Secure','🎯 Smart','💡 Easy'].map(x=>`<div style="padding:18px 12px;border-radius:18px;background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.3);font-weight:700;font-size:15px;box-shadow:0 8px 30px rgba(168,85,247,.6)">${x}</div>`).join('')}
    </div></div></div>`,
2:`<div style="position:absolute;inset:0;background:radial-gradient(circle at 20% 15%,#4c1d95 0,transparent 45%),radial-gradient(circle at 90% 70%,#0e7490 0,transparent 45%),#07041a;color:#fff;font-family:Inter;overflow:hidden">
  <div style="padding:64px 22px 0">
    <div style="font-size:13px;opacity:.6">Good morning 👋</div>
    <div style="font-size:30px;font-weight:800;margin-top:4px">Dashboard <span style="color:#22d3ee;text-shadow:0 0 18px #22d3ee">AI</span></div>
    <div style="margin-top:20px;padding:20px;border-radius:24px;background:rgba(255,255,255,.07);border:1px solid rgba(255,255,255,.18);box-shadow:0 0 50px rgba(34,211,238,.35)">
      <div style="font-size:12px;opacity:.6">Total Revenue</div><div style="font-size:34px;font-weight:800;margin-top:4px">$12,480 <span style="font-size:14px;color:#4ade80">↑ 24%</span></div>
      <svg viewBox="0 0 240 70" style="width:100%;margin-top:8px"><path d="M0 60 C40 50 50 20 90 35 S150 5 240 12" fill="none" stroke="#a855f7" stroke-width="4" style="filter:drop-shadow(0 0 8px #a855f7)"/></svg></div>
    ${['🚀 Boost Productivity','🤖 AI Insights','🔮 Smart Predictions'].map(x=>`<div style="margin-top:12px;padding:16px;border-radius:18px;background:rgba(255,255,255,.06);border:1px solid rgba(168,85,247,.5);box-shadow:0 0 24px rgba(168,85,247,.45);font-weight:700">${x}</div>`).join('')}
  </div></div>`,
3:`<div style="position:absolute;inset:0;background:#faf5ff;font-family:Inter;overflow:hidden;color:#1e1b4b">
  <div style="padding:64px 22px 0;text-align:center">
    <div style="font-size:34px;font-weight:900;background:linear-gradient(90deg,#7c3aed,#db2777,#f59e0b);-webkit-background-clip:text;color:transparent">Why Choose Us? 🔥</div>
    <div style="font-size:14px;opacity:.6;margin-top:8px">Next-gen features for next-gen people</div>
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:22px">
      ${['⚡','🔒','🎯','🚀','💡','✨'].map((e,i)=>`<div style="padding:20px 10px;border-radius:22px;background:#fff;box-shadow:0 12px 30px rgba(124,58,237,.35);transform:rotate(${i%2?2:-2}deg)"><div style="font-size:34px">${e}</div><div style="font-weight:800;font-size:14px;margin-top:6px">${['Lightning','Secure','Precise','Scalable','Innovative','Magical'][i]}</div></div>`).join('')}
    </div></div></div>`,
};
