import{A as y}from"./arrow-left-DOlK7rXr.js";import{c as n,d as m,a as i,b as s,e as k,u as o,F as C,i as h,r as v,o as t,n as l,j as d,t as c,_ as p}from"./index-CtSJwEFs.js";/**
 * @license lucide-vue-next v0.359.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const f=n("CircleCheckIcon",[["circle",{cx:"12",cy:"12",r:"10",key:"1mglay"}],["path",{d:"m9 12 2 2 4-4",key:"dzmm74"}]]);/**
 * @license lucide-vue-next v0.359.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const I=n("CircleXIcon",[["circle",{cx:"12",cy:"12",r:"10",key:"1mglay"}],["path",{d:"m15 9-6 6",key:"1uzhvr"}],["path",{d:"m9 9 6 6",key:"z0biqf"}]]);/**
 * @license lucide-vue-next v0.359.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const g=n("ClockIcon",[["circle",{cx:"12",cy:"12",r:"10",key:"1mglay"}],["polyline",{points:"12 6 12 12 16 14",key:"68esgv"}]]),A={class:"history-view"},S={class:"header-nav"},N={class:"toss-card"},b={class:"history-list"},D={class:"item-left"},E={class:"item-order"},z={class:"item-date"},P={class:"item-right"},L={class:"item-amount"},O=m({__name:"HistoryView",setup(R){const u=v([{id:1,orderId:"ORD-982141",amount:5e4,status:"SUCCESS",createdAt:"2026-07-25 21:10:00"},{id:2,orderId:"ORD-881240",amount:12e3,status:"APPROVING",createdAt:"2026-07-25 21:08:12"},{id:3,orderId:"ORD-712399",amount:33e3,status:"CANCELED",createdAt:"2026-07-25 20:30:45"},{id:4,orderId:"ORD-612091",amount:99e3,status:"FAILED",createdAt:"2026-07-25 19:15:22"}]),_=r=>{switch(r){case"SUCCESS":return"toss-badge-success";case"PENDING":case"APPROVING":return"toss-badge-pending";default:return"toss-badge-failed"}};return(r,a)=>(t(),i("div",A,[s("div",S,[s("button",{class:"back-btn",onClick:a[0]||(a[0]=e=>r.$router.push("/"))},[k(o(y),{size:22})]),a[1]||(a[1]=s("h2",{class:"nav-title"},"결제 이력 조회",-1))]),s("div",N,[s("div",b,[(t(!0),i(C,null,h(u.value,e=>(t(),i("div",{key:e.id,class:"history-item"},[s("div",D,[s("div",{class:l(["item-icon",e.status.toLowerCase()])},[e.status==="SUCCESS"?(t(),d(o(f),{key:0,size:20})):e.status==="PENDING"||e.status==="APPROVING"?(t(),d(o(g),{key:1,size:20})):(t(),d(o(I),{key:2,size:20}))],2),s("div",null,[s("div",E,c(e.orderId),1),s("div",z,c(e.createdAt),1)])]),s("div",P,[s("div",L,c(e.amount.toLocaleString())+" 원",1),s("span",{class:l(["toss-badge",_(e.status)])},c(e.status),3)])]))),128))])])]))}}),x=p(O,[["__scopeId","data-v-93f66ff2"]]);export{x as default};
