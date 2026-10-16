// Deterministic frame engine: render(t) sets every element's state for time t.
const clamp=(v,a=0,b=1)=>Math.min(b,Math.max(a,v));
const lerp=(a,b,p)=>a+(b-a)*p;
const Ease={
  lin:x=>x,
  out:x=>1-Math.pow(1-x,3),
  expo:x=>x>=1?1:1-Math.pow(2,-10*x),
  back:x=>{const c1=1.9,c3=c1+1;return 1+c3*Math.pow(x-1,3)+c1*Math.pow(x-1,2)},
  inout:x=>x<.5?4*x*x*x:1-Math.pow(-2*x+2,3)/2,
  in:x=>x*x*x,
  spring:x=>x>=1?1:1-Math.exp(-6*x)*Math.cos(10*x),
};
const TRACKS=[],HOOKS=[],SFX=[];
const $=s=>document.querySelector(s);
const TF=['x','y','s','r','o','b','sx','sy'];
const DEF={x:0,y:0,s:1,r:0,o:1,b:0,sx:1,sy:1};
// K(sel,[[t,{props},ease]...]) — props merge forward from key to key.
function K(sel,keys){
  const el=typeof sel==='string'?$(sel):sel;
  if(!el){console.error('missing',sel);return}
  keys=[...keys].sort((a,b)=>a[0]-b[0]);
  let cur={...DEF};const res=[];
  for(const [t,p,e] of keys){cur={...cur,...p};res.push([t,{...cur},e||'expo'])}
  TRACKS.push({el,res});
}
// show(sel,a,b,opts): enter at a, exit at b
function show(sel,a,b,o={}){
  const from={o:0,y:50,s:.94,...(o.from||{})}, rest={x:0,y:0,s:1,r:0,o:1,b:0,...(o.rest||{})};
  const exit={o:0,y:-40,s:.98,...(o.exit||{})};
  const keys=[[a,from],[a+(o.dur||.5),rest,o.ease||'expo']];
  if(b!=null) keys.push([b,{}],[b+(o.outDur||.3),exit,o.outEase||'in']);
  K(sel,keys);
}
function sfx(t,name,gain=1){SFX.push([+t.toFixed(3),name,gain])}
function evalTrack(tr,t){
  const r=tr.res;
  if(t<=r[0][0]) return r[0][1];
  for(let i=0;i<r.length-1;i++){
    const [t0,a]=r[i],[t1,b,e]=r[i+1];
    if(t<=t1){
      const p=t1===t0?1:Ease[e](clamp((t-t0)/(t1-t0)));
      const o={};for(const k in b){o[k]=typeof b[k]==='number'&&typeof a[k]==='number'?lerp(a[k],b[k],p):(p<.5?a[k]:b[k])}
      return o;
    }
  }
  return r[r.length-1][1];
}
function apply(el,p){
  const st=el.style;
  if(p.o<=.002){st.display='none';return}
  st.display='';
  st.opacity=clamp(p.o);
  st.transform=`translate(${p.x}px,${p.y}px) rotate(${p.r}deg) scale(${p.s*p.sx},${p.s*p.sy})`;
  st.filter=p.b>.05?`blur(${p.b}px)`:'';
  if('L' in p){st.left=p.L+'px';st.top=p.T+'px';st.width=p.W+'px';st.height=p.H+'px';st.borderRadius=p.R+'px'}
  if('cl' in p) st.clipPath=`inset(0 ${(1-p.cl)*100}% 0 0)`;
  if('ct' in p) st.clipPath=`inset(${(1-p.ct)*100}% 0 0 0)`;
}
// ---- captions ----
function buildChunks(words,maxW=3){
  const out=[];let cur=[];
  words.forEach((w,i)=>{
    cur.push(w);
    const nx=words[i+1];
    const brk=!nx||cur.length>=maxW||/[.,!?]$/.test(w.w)||(nx.s-w.e>.45)||w.w.length>11&&cur.length>=2;
    if(brk){out.push(cur);cur=[]}
  });
  return out.map((c,i)=>({words:c,s:c[0].s,e:c[c.length-1].e,next:out[i+1]?out[i+1][0].s:1e9}));
}
let CHUNKS=[],capHidden=[];
function caption(t){
  const cap=$('#cap');
  const ch=CHUNKS.find(c=>t>=c.s-.04&&t<Math.min(c.next,c.e+.7));
  const hidden=capHidden.some(([a,b])=>t>=a&&t<b);
  if(!ch||hidden){cap.style.visibility='hidden';cap.dataset.k='';return}
  cap.style.visibility='visible';
  const key=ch.s+'';
  if(cap.dataset.k!==key){cap.innerHTML=ch.words.map(w=>`<span>${w.w.replace(/[.,]$/,'')}</span>`).join('');cap.dataset.k=key}
  [...cap.children].forEach((sp,i)=>{
    const w=ch.words[i];
    sp.className=t>=w.s-.03?(t<(ch.words[i+1]?ch.words[i+1].s:w.e+.25)?'on now':'on'):'';
  });
  const p=clamp((t-ch.s+.04)/.22);
  cap.style.transform=`scale(${lerp(.86,1,Ease.back(p))})`;
  cap.style.opacity=clamp(p*3);
}
// ---- face frames ----
let FACE_DIR='',FACE_N=0,lastFace=-1;
async function setFace(t){
  const n=Math.min(FACE_N,Math.max(1,Math.round(t*30)+1));
  if(n===lastFace) return;lastFace=n;
  const img=$('#faceimg');img.src=`${FACE_DIR}/${String(n).padStart(5,'0')}.jpg`;
  try{await img.decode()}catch(e){}
}
window.render=async function(t){
  await setFace(t);
  const acc=new Map();
  for(const tr of TRACKS){
    const v=evalTrack(tr,t),a=acc.get(tr.el);
    if(!a){acc.set(tr.el,{...v});continue}
    a.x+=v.x;a.y+=v.y;a.r+=v.r;a.b+=v.b;a.s*=v.s;a.sx*=v.sx;a.sy*=v.sy;a.o*=v.o;
    for(const k of ['L','T','W','H','R','cl','ct']) if(k in v) a[k]=v[k];
  }
  for(const [el,v] of acc) apply(el,v);
  for(const h of HOOKS) h(t);
  caption(t);
};
window.ready=async function(){
  await document.fonts.ready;
  await Promise.all([...document.images].map(i=>i.complete?0:new Promise(r=>{i.onload=i.onerror=r})));
};
// face layouts (left,top,width,height,radius)
const LAY={
  full:{L:0,T:0,W:720,H:1280,R:0},
  card:{L:40,T:760,W:640,H:480,R:34},
  cardTop:{L:40,T:40,W:640,H:420,R:34},
  pip:{L:478,T:996,W:206,H:244,R:30},
  circle:{L:262,T:610,W:196,H:196,R:98},
};
// typing helper
function typer(sel,text,a,cps=28){const el=$(sel);HOOKS.push(t=>{const n=clamp(Math.floor((t-a)*cps),0,text.length);el.textContent=text.slice(0,n)+(t>a&&n<text.length&&Math.floor(t*4)%2?'▍':'')})}
function counter(sel,to,a,d=.9,fmt=v=>Math.round(v)){const el=$(sel);HOOKS.push(t=>{el.textContent=fmt(to*Ease.out(clamp((t-a)/d)))})}
