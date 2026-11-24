const crypto = require("node:crypto");

const token = () => String(process.env.GITHUB_DATA_TOKEN || process.env.GITHUB_TOKEN || process.env.GITHUB_UPLOAD_TOKEN || "").trim();
const repo = () => String(process.env.GITHUB_DATA_REPOSITORY || process.env.GITHUB_UPLOAD_REPOSITORY || "khaiwilsan676-alt/Zyora-Server").trim();
const branch = () => String(process.env.GITHUB_DATA_BRANCH || "main").trim();
const configured = () => Boolean(token() && /^[\w.-]+\/[\w.-]+$/.test(repo()));
const enc = (v) => Buffer.from(JSON.stringify(v, null, 2)).toString("base64");
const dec = (v) => JSON.parse(Buffer.from(v || "", "base64").toString("utf8"));
const pathFor = (name) => "database/" + name.replace(/[^a-zA-Z0-9_-]/g, "_") + ".json";

async function request(path, options = {}) {
  const r = await fetch("https://api.github.com/repos/" + repo() + "/contents/" + path, {
    ...options,
    headers: { Accept: "application/vnd.github+json", Authorization: "Bearer " + token(), "X-GitHub-Api-Version": "2022-11-28", "Content-Type": "application/json", ...(options.headers || {}) }
  });
  const body = await r.json().catch(() => ({}));
  if (!r.ok && !(r.status === 404 && options.allow404)) { const e = new Error(body.message || "GitHub document store request failed (" + r.status + ")"); e.status = r.status; throw e; }
  return { r, body };
}
function val(o, path) { return path.split(".").reduce((v, k) => v == null ? undefined : v[k], o); }
function equal(a,b) { if (a instanceof Date) a = a.getTime(); if (b instanceof Date) b = b.getTime(); if (a && typeof a === "object" || b && typeof b === "object") return JSON.stringify(a) === JSON.stringify(b); return a === b; }
function matches(doc, q = {}) {
  return Object.entries(q).every(([k,want]) => {
    if (k === "$or") return want.some(x => matches(doc,x));
    if (k === "$and") return want.every(x => matches(doc,x));
    const got = val(doc,k);
    if (want && typeof want === "object" && !Array.isArray(want) && !(want instanceof Date)) {
      return Object.entries(want).every(([op,x]) => {
        if (op === "$exists") return x ? got !== undefined : got === undefined;
        if (op === "$ne") return !equal(got,x);
        if (op === "$in") return x.some(y => Array.isArray(got) ? got.some(z=>equal(z,y)) : equal(got,y));
        if (op === "$nin") return !x.some(y => equal(got,y));
        if (op === "$gt") return got > x;
        if (op === "$gte") return got >= x;
        if (op === "$lt") return got < x;
        if (op === "$lte") return got <= x;
        if (op === "$elemMatch") return Array.isArray(got) && got.some(y=>matches(y,x));
        return op.startsWith("$") ? false : equal(got,want);
      });
    }
    return Array.isArray(got) ? got.some(x=>equal(x,want)) : equal(got,want);
  });
}
function setPath(obj,path,value) { const p=path.split("."); let o=obj; for(let i=0;i<p.length-1;i++) o=o[p[i]]||(o[p[i]]={}); o[p[p.length-1]]=value; }
function unsetPath(obj,path) { const p=path.split("."); let o=obj; for(let i=0;i<p.length-1;i++) { if(!o[p[i]]) return; o=o[p[i]]; } delete o[p[p.length-1]]; }
function apply(doc, update, inserting=false) {
  if (!Object.keys(update).some(k=>k.startsWith("$"))) { const id=doc._id; return {...update,_id:update._id||id}; }
  for (const [op,fields] of Object.entries(update)) for (const [k,v] of Object.entries(fields||{})) {
    if(op==="$set" || (op==="$setOnInsert" && inserting)) setPath(doc,k,v);
    else if(op==="$inc") setPath(doc,k,(Number(val(doc,k))||0)+Number(v));
    else if(op==="$unset") unsetPath(doc,k);
    else if(op==="$push") { const a=val(doc,k); const next=Array.isArray(a)?a:[]; next.push(v); setPath(doc,k,next); }
    else if(op==="$addToSet") { const a=val(doc,k); const next=Array.isArray(a)?a:[]; if(!next.some(x=>equal(x,v))) next.push(v); setPath(doc,k,next); }
    else if(op==="$pull") { const a=val(doc,k); if(Array.isArray(a)) setPath(doc,k,a.filter(x=>!(v&&typeof v==="object"?matches(x,v):equal(x,v)))); }
  }
  return doc;
}
function projection(doc, p) { if(!p) return doc; const keys=Object.keys(p); if(keys.length && keys.every(k=>!p[k])) { const d={...doc}; keys.forEach(k=>delete d[k]); return d; } const out={}; for(const k of keys) if(p[k] && val(doc,k)!==undefined) setPath(out,k,val(doc,k)); if(doc._id!==undefined && p._id!==0) out._id=doc._id; return out; }

class Collection {
  constructor(name) { this.name=name; this.path=pathFor(name); this.queue=Promise.resolve(); }
  async load() {
    if(!configured()) throw new Error("Set GITHUB_DATA_TOKEN and grant contents read/write access to the private Zyora-Server repository.");
    const {r,body}=await request(this.path,{allow404:true});
    if(r.status===404) return {docs:[],sha:null};
    const data=dec(body.content);
    return {docs:Array.isArray(data.docs)?data.docs:[],sha:body.sha};
  }
  async save(docs,sha) {
    const payload={message:"Update Zyora database collection: "+this.name,content:enc({version:1,updatedAt:new Date().toISOString(),docs}),branch:branch(),...(sha?{sha}:{})};
    try { const {body}=await request(this.path,{method:"PUT",body:JSON.stringify(payload)}); return body; }
    catch(e) { if(e.status===404 && !sha) throw e; throw e; }
  }
  async mutate(fn) { const run=this.queue.then(async()=>{ for(let n=0;n<4;n++){ const x=await this.load(); const out=await fn(x.docs); try { const saved=await this.save(x.docs,x.sha); return {...out, acknowledged:true, commit:saved.commit&&saved.commit.sha}; } catch(e){if(![409,422].includes(e.status)||n===3) throw e;} } }); this.queue=run.catch(()=>{}); return run; }
  async findOne(q={},opts={}) { const {docs}=await this.load(); const d=docs.find(x=>matches(x,q)); return d?projection(d,opts.projection||opts):null; }
  find(q={}) { const c=this; const cursor={q,sortSpec:null,cap:Infinity,proj:null,sort(s){this.sortSpec=s;return this;},limit(n){this.cap=n;return this;},project(p){this.proj=p;return this;},async toArray(){let {docs}=await c.load(); docs=docs.filter(x=>matches(x,this.q)); if(this.sortSpec){const ent=Object.entries(this.sortSpec);docs.sort((a,b)=>{for(const [k,d] of ent){const av=val(a,k),bv=val(b,k);if(equal(av,bv))continue;return (av>bv?1:-1)*d;}return 0;});} return docs.slice(0,this.cap).map(x=>projection(x,this.proj));}}; return cursor; }
  async insertOne(doc) { const d={...doc,_id:doc._id||crypto.randomUUID()}; return this.mutate(docs=>{docs.push(d);return {insertedId:d._id};}); }
  async updateOne(q,update,opts={}) { return this.mutate(docs=>{let i=docs.findIndex(d=>matches(d,q));let inserted=false;if(i<0&&opts.upsert){const base={};for(const [k,v] of Object.entries(q))if(!k.startsWith("$")&&(!v||typeof v!=="object"||v instanceof Date))setPath(base,k,v);base._id=crypto.randomUUID();docs.push(base);i=docs.length-1;inserted=true;}if(i<0)return {matchedCount:0,modifiedCount:0,upsertedId:null};const before=JSON.stringify(docs[i]);docs[i]=apply(docs[i],update,inserted);return {matchedCount:inserted?0:1,modifiedCount:before===JSON.stringify(docs[i])?0:1,upsertedId:inserted?docs[i]._id:null};}); }
  async updateMany(q,update) { return this.mutate(docs=>{let n=0;docs.forEach((d,i)=>{if(matches(d,q)){docs[i]=apply(d,update);n++;}});return {matchedCount:n,modifiedCount:n};}); }
  async deleteOne(q) { return this.mutate(docs=>{const i=docs.findIndex(d=>matches(d,q));if(i<0)return {deletedCount:0};docs.splice(i,1);return {deletedCount:1};}); }
  async countDocuments(q={}) { const {docs}=await this.load();return docs.filter(d=>matches(d,q)).length; }
}
class GitHubDocumentDB {
  constructor() { this.collections=new Map(); }
  collection(name) { if(!this.collections.has(name))this.collections.set(name,new Collection(name));return this.collections.get(name); }
}
class GitHubClient {
  async connect() { if(!configured()) throw new Error("GitHub database not configured: set GITHUB_DATA_TOKEN with Contents read/write permission."); }
  db() { return new GitHubDocumentDB(); }
  async close() {}
}
module.exports={GitHubClient};
