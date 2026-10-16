// usage: node render.js page.html outdir [times...]  (no times = full render at 30fps)
const { chromium } = require('playwright'); const fs=require('fs'); const path=require('path');
(async()=>{
 const [html,out,...times]=process.argv.slice(2); fs.mkdirSync(out,{recursive:true});
 const b=await chromium.launch({executablePath:'/opt/pw-browsers/chromium-1194/chrome-linux/chrome',args:['--allow-file-access-from-files']});
 const p=await b.newPage({viewport:{width:720,height:1280},deviceScaleFactor:1.5});
 p.on('console',m=>{if(m.type()==='error')console.log('PAGE',m.text())}); p.on('pageerror',e=>console.log('ERR',e.message));
 await p.goto('file://'+path.resolve(html)); await p.evaluate(()=>window.ready());
 const dur=await p.evaluate(()=>window.DURATION);
 fs.writeFileSync(path.join(out,'sfx.json'),JSON.stringify(await p.evaluate(()=>window.SFX)));
 const list=times.length?times.map(Number):[...Array(Math.round(dur*30)).keys()].map(i=>i/30);
 let i=0;
 for(const t of list){
   await p.evaluate(t=>window.render(t),t);
   const name=times.length?`t_${t.toFixed(2)}.jpg`:`${String(i+1).padStart(5,'0')}.jpg`;
   await p.screenshot({path:path.join(out,name),type:'jpeg',quality:93}); i++;
   if(i%150===0) console.log(out,i,'/',list.length);
 }
 await b.close();
})();
