"use strict";(self.webpackChunkeasy_extension=self.webpackChunkeasy_extension||[]).push([[688],{6466:function(ce,D,e){e.d(D,{Z:function(){return H}});var z=e(7134),M=e(67294),R={A:"#8E44AD",B:"#3498DB",C:"#E74C3C",D:"#2ECC71",E:"#F39C12",F:"#9B59B6",G:"#1ABC9C",H:"#D35400",I:"#C0392B",J:"#27AE60",K:"#7D3C98",L:"#FFC300",M:"#5D6D7E",N:"#16A085",O:"#F1C40F",P:"#884EA0",Q:"#2980B9",R:"#E67E22",S:"#48C9B0",T:"#B03A2E",U:"#3A539B",V:"#D2B4DE",W:"#7F8C8D",X:"#A9DFBF",Y:"#EC7063",Z:"#45B39D"},m="#E74C3C",w=e(85893),W=function(U){var G=U.letter,f=R[G||"C"]||m;return(0,w.jsx)(z.C,{style:{backgroundColor:f},children:G})},H=W},24098:function(ce,D,e){e.d(D,{Z:function(){return ue}});var z=e(19632),M=e.n(z),R=e(5574),m=e.n(R),w=function(t,ee,te){if(t===void 0||t.length===0)return[];var J=(ee-1)*te,se=J+te;return t.slice(J,se)},W=e(90930),H=e(52715),B=e(21725),U=e(69618),G=e(738),f=e(67294),Y=e(11443),k=e(39711),T=e(49677),q=e.n(T),ge=e(19826),xe=(0,ge.kc)(function(x){return q()(x),{searchContentContainer:{textAlign:"center"},searchContent:{maxWidth:522,width:"100%",margin:"25px 0"},cardList:{".ant-list .ant-list-item-content-single":{maxWidth:"100%"}},pagination:{marginTop:"30px",display:"flex",justifyContent:"center"}}}),_=xe,g=e(85893);function pe(x){var t=x.id,ee=x.searchPlaceholderI18nId,te=x.pageHeadContent,J=x.initPageNo,se=J===void 0?1:J,ve=x.initPageSize,oe=ve===void 0?8:ve,re=x.dataFetcher,ye=x.searchItemFilter,me=x.renderItem,ne=oe,Ce=_({}),ae=Ce.styles,ie=(0,Y.Z)(),O=(0,k.lr)(),a=m()(O,2),r=a[0],u=a[1],A=(0,f.useState)(""),p=m()(A,2),Z=p[0],v=p[1],y=(0,f.useState)(!1),C=m()(y,2),F=C[0],L=C[1],S=(0,f.useState)([]),N=m()(S,2),h=N[0],V=N[1],le=(0,f.useState)([]),b=m()(le,2),n=b[0],s=b[1],l=(0,f.useState)(1),d=m()(l,2),o=d[0],I=d[1],$=function(i){I(i)},Se=function(i){v(i.target.value)};(0,f.useEffect)(function(){s(w(h,o,ne))},[o]),(0,f.useEffect)(function(){if((o-1)*ne>h.length){var E=Math.ceil(h.length/ne);I(E)}else s(w(h,o,ne))},[h]);var he=function(i,P){return i==null?void 0:i.filter(function(Q){return Q?ye(Q,P):!0})},fe=(0,H.useRequest)(re,{onSuccess:function(i){var P=r.get("keyword")||"",Q=he(i,P);V(Q),v(P)}}),K=fe.data,c=fe.loading,j=function(i){L(!0),u({keyword:i}),v(i);var P=he(K||[],i);V(P),setTimeout(function(){L(!1)},100)},X=(0,g.jsxs)(g.Fragment,{children:[te,(0,g.jsx)("div",{className:ae.searchContentContainer,children:(0,g.jsx)(B.Z.Search,{placeholder:ie.formatMessage({id:ee}),enterButton:ie.formatMessage({id:"component.search.button-name"}),size:"large",onSearch:j,className:ae.searchContent,value:Z,onChange:Se})})]});return(0,g.jsxs)(W._z,{content:X,children:[(0,g.jsx)("div",{className:ae.cardList,children:(0,g.jsx)(U.Z,{rowKey:t,loading:c||F,grid:{gutter:16,xs:1,sm:2,md:3,lg:3,xl:4,xxl:4},dataSource:M()(n),renderItem:function(i,P){return(0,g.jsx)(U.Z.Item,{children:me&&me(i,P)},P)}})}),(0,g.jsx)("div",{className:ae.pagination,children:(0,g.jsx)(G.Z,{current:o,pageSize:oe,onChange:$,defaultCurrent:se,defaultPageSize:oe,total:h.length,showTotal:function(i){return(0,g.jsx)("p",{style:{transform:"translateY(2px)"},children:ie.formatMessage({id:"component.pagination.show-total"},{total:i})})}})})]})}var ue=pe},4122:function(ce,D,e){e.r(D),e.d(D,{default:function(){return ie}});var z=e(5574),M=e.n(z),R=e(90542),m=e(6466),w=e(24098),W=e(72498),H=e(55394),B=e(10305),U=e(52715),G=e(74470),f=e(4393),Y=e(92797),k=e(41730),T=e(67294),q=e(11443),ge=e(68400),xe=e.n(ge),_=e(83062),g=e(66309),pe=e(56604),ue=e(19826),x=e(43439),t=e(85893),ee,te=(0,ue.kc)(function(O){var a=O.css,r="ant";return{rowBiz:{backgroundColor:"#ffe7e7"},customTable:a(ee||(ee=xe()([`
      `,`-table {
        `,`-table-container {
          `,`-table-body,
          `,`-table-content {
            scrollbar-width: thin;
            scrollbar-color: #eaeaea transparent;
            scrollbar-gutter: stable;
          }
        }
      }
    `])),r,r,r,r)}}),J=function(a,r,u,A,p,Z,v){var y,C=new Map;r.forEach(function(n){C.set(n.code,n)});var F=new Map;u.forEach(function(n){var s;F.set((n==null||(s=n.classInfo)===null||s===void 0?void 0:s.fullName)||"",n)});var L=[{title:A,width:100,dataIndex:"code",key:"code",fixed:"left",ellipsis:!0,render:function(s,l){var d,o,I=l.code,$=l.instanceType;return $==="business"?(0,t.jsx)(_.Z,{title:(0,t.jsx)("pre",{children:a==null||(d=a.classInfo)===null||d===void 0?void 0:d.comment}),overlayStyle:{maxWidth:"800px"},children:(0,t.jsx)("p",{children:I})}):(0,t.jsx)(_.Z,{title:(0,t.jsx)("pre",{children:(o=C.get(I))===null||o===void 0||(o=o.classInfo)===null||o===void 0?void 0:o.comment}),overlayStyle:{maxWidth:"800px"},children:(0,t.jsx)("a",{href:(0,H.t)("abilities",{keyword:"code:"+I}),target:"_blank",rel:"noreferrer",children:I})})}},{title:p,width:80,dataIndex:"priority",key:"priority",ellipsis:!0},{title:Z,width:80,dataIndex:"instanceType",key:"instanceType",ellipsis:!0,render:function(s,l){var d=l.instanceType;return(0,t.jsx)(g.Z,{color:d==="business"?"volcano":"green",children:d})}}],S=[],N={key:"biz::"+a.code,code:a.code,priority:a.priority,instanceType:"business"},h=new Set([]);a.implExtensionPoints.forEach(function(n){N[n]="\u2705",h.has(n)||h.add(n)}),S.push(N),(y=a.usedAbilities)===null||y===void 0||y.forEach(function(n){var s,l=C.get(n.abilityCode),d={key:"ability::"+n.abilityCode,code:n.abilityCode,priority:n.priority,instanceType:"ability"};l==null||(s=l.implExtensionPoints)===null||s===void 0||s.forEach(function(o){d[o]="\u2705",h.has(o)||h.add(o)}),S.push(d)}),S.sort(function(n,s){return n.priority-s.priority});var V=Array.from(h);V.sort(function(n,s){var l=(0,B.t)(n),d=(0,B.t)(s);return l===d?n.localeCompare(s):l.localeCompare(d)});var le=S.length,b="#ff6666";return V.forEach(function(n){var s,l=0;S.forEach(function(o){o[n]==="\u2705"&&(l+=1)});var d=(0,t.jsxs)(t.Fragment,{children:[(0,t.jsx)(_.Z,{title:(0,t.jsx)("pre",{children:(s=F.get(n))===null||s===void 0||(s=s.classInfo)===null||s===void 0?void 0:s.comment}),overlayStyle:{maxWidth:"800px"},children:(0,t.jsx)("a",{href:(0,H.t)("extension-points",{keyword:"fullName:"+n}),target:"_blank",rel:"noreferrer",children:(0,B.t)(n)})}),l>1&&(0,t.jsx)(_.Z,{title:v,children:(0,t.jsx)(x.Z,{style:{marginLeft:5},twoToneColor:b})})]});L.push({title:d,width:160,dataIndex:n,key:n,ellipsis:!0,onCell:function(I,$){return l<=1?{}:{style:{borderLeft:"1px solid ".concat(b),borderRight:"1px solid ".concat(b),borderBottom:$===le-1?"1px solid ".concat(b):""}}},onHeaderCell:function(I,$){return l<=1?{}:{style:{borderTop:"1px solid ".concat(b),borderLeft:"1px solid ".concat(b),borderRight:"1px solid ".concat(b)}}}})}),{dataSource:S,columns:L}},se=function(a){var r=a.businessInfo,u=a.abilities,A=a.extensionPoints,p=te(),Z=p.styles,v=(0,q.Z)(),y=v.formatMessage({id:"page.businesses.extension-point-info-table.column.ext-impl"}),C=v.formatMessage({id:"page.businesses.extension-point-info-table.column.priority"}),F=v.formatMessage({id:"page.businesses.extension-point-info-table.column.ext-type"}),L=v.formatMessage({id:"page.businesses.extension-point-info-table.conflict.note"}),S=J(r,u,A,y,C,F,L),N=S.columns,h=S.dataSource;return(0,t.jsxs)(t.Fragment,{children:[(0,t.jsx)("div",{style:{marginBottom:2,fontSize:12,color:"#565656"},children:v.formatMessage({id:"page.businesses.extension-point-info-table.desc"})}),(0,t.jsx)("div",{style:{marginBottom:10,fontSize:12,color:"#565656"},children:v.formatMessage({id:"page.businesses.extension-point-info-table.tip"})}),(0,t.jsx)(pe.Z,{className:Z.customTable,pagination:!1,columns:N,dataSource:h,scroll:{x:"max-content"}})]})},ve=se,oe=e(9783),re=e.n(oe),ye=(0,ue.kc)(function(O){var a=O.token;return{pageHeaderContent:re()({position:"relative"},"@media screen and (max-width: ".concat(a.screenSM,"px)"),{paddingBottom:"30px"}),contentLink:re()(re()({height:"30px",marginTop:"16px",a:{marginRight:"32px",img:{width:"24px"}},img:{marginRight:"5px",verticalAlign:"middle"}},"@media screen and (max-width: ".concat(a.screenLG,"px)"),{a:{marginRight:"16px"}}),"@media screen and (max-width: ".concat(a.screenSM,"px)"),{position:"absolute",bottom:"-4px",left:"0",width:"1000px",a:{marginRight:"16px"},img:{marginRight:"4px"}}),card:{".ant-card-meta-title":{marginBottom:"12px","& > a":{display:"inline-block",maxWidth:"100%",color:a.colorTextHeading}},".ant-card-body:hover":{".ant-card-meta-title > a":{color:a.colorPrimary}}},item:{height:"64px",whiteSpace:"pre-line"},extensionImplementationContent:{listStyleType:"disc",listStylePosition:"outside",paddingLeft:"15px"},extensionImplementationBtn:{width:"100%",height:"100%"}}}),me=ye,ne=G.Z.Paragraph,Ce=function(a,r){var u,A;if(!a||!r)return!0;var p=r.split(":");if(p.length>1){var Z=p[0],v=p[1];if(Z==="code")return a.code===v;if(Z==="fullName"){var y;return((y=a.classInfo)===null||y===void 0?void 0:y.name)===v}}var C=a.code||"",F=((u=a.classInfo)===null||u===void 0?void 0:u.name)||"",L=((A=a.classInfo)===null||A===void 0?void 0:A.comment)||"";return C.toLowerCase().includes(r.toLowerCase())||F.toLowerCase().includes(r.toLowerCase())||L.toLowerCase().includes(r.toLowerCase())},ae=function(){var a=me({}),r=a.styles,u=(0,q.Z)(),A=(0,T.useState)(800),p=M()(A,2),Z=p[0],v=p[1],y=(0,T.useState)([]),C=M()(y,2),F=C[0],L=C[1];(0,T.useEffect)(function(){(0,W.Al)().then(function(K){var c=K.data;L(c)})},[]);var S=(0,T.useState)([]),N=M()(S,2),h=N[0],V=N[1];(0,T.useEffect)(function(){(0,W.jP)().then(function(K){var c=K.data;V(c)})},[]);var le=(0,T.useState)(!1),b=M()(le,2),n=b[0],s=b[1],l=(0,T.useState)({title:"",content:null}),d=M()(l,2),o=d[0],I=d[1],$=function(c,j){var X=arguments.length>2&&arguments[2]!==void 0?arguments[2]:800;s(!0),I({title:c,content:j}),v(X)},Se=function(){s(!1),I({title:"",content:null})},he=(0,t.jsx)("div",{className:r.pageHeaderContent,children:(0,t.jsx)("p",{children:(0,t.jsx)(U.FormattedMessage,{id:"page.businesses.desc"})})}),fe=function(c){var j,X,E,i,P,Q,be=(j=c.classInfo)===null||j===void 0||(j=j.name)===null||j===void 0||(j=j.charAt(0))===null||j===void 0?void 0:j.toUpperCase(),je=u.formatMessage({id:"page.businesses.card-modal.source-code"}),Pe=((X=c.classInfo)===null||X===void 0?void 0:X.sourceCode)&&(0,t.jsx)(R.P4,{code:((E=c.classInfo)===null||E===void 0?void 0:E.sourceCode)||"",language:"java"}),Ie=u.formatMessage({id:"page.businesses.card-modal.ext-impl"}),Ee=((i=c.implExtensionPoints)===null||i===void 0?void 0:i.length)>0?(0,t.jsx)("ul",{className:r.extensionImplementationContent,children:(P=c.implExtensionPoints)===null||P===void 0?void 0:P.map(function(de,Ae){return(0,t.jsx)("li",{children:(0,t.jsx)("a",{href:(0,H.t)("extension-points",{keyword:"fullName:"+de}),target:"_blank",rel:"noreferrer",children:(0,B.t)(de)})},Ae)})}):(0,t.jsx)("p",{children:u.formatMessage({id:"page.businesses.card-modal.none-abilities"})}),Me=u.formatMessage({id:"page.businesses.card-modal.used-abilities"}),Te=(0,t.jsx)(ve,{businessInfo:c,abilities:F,extensionPoints:h});return(0,t.jsx)(f.Z,{hoverable:!0,className:r.card,actions:[(0,t.jsx)(Y.ZP,{color:"primary",variant:"text",onClick:function(){return $(je,Pe)},children:u.formatMessage({id:"page.businesses.card-btn.source-code"})},"source-code"),(0,t.jsx)(Y.ZP,{color:"primary",variant:"text",onClick:function(){return $(Ie,Ee)},children:u.formatMessage({id:"page.businesses.card-btn.ext-impl"})},"extension-implementation"),(0,t.jsx)(Y.ZP,{color:"primary",variant:"text",onClick:function(){return $(Me,Te,1e3)},children:u.formatMessage({id:"page.businesses.card-btn.used-abilities"})},"used-abilities")],children:(0,t.jsx)(f.Z.Meta,{avatar:(0,t.jsx)(m.Z,{letter:be}),title:(0,t.jsx)("a",{children:c.code}),description:(0,t.jsx)(ne,{className:r.item,ellipsis:{rows:4},children:(Q=c.classInfo)===null||Q===void 0?void 0:Q.comment})})})};return(0,t.jsxs)(t.Fragment,{children:[(0,t.jsx)(w.Z,{id:function(c){return c.code},searchPlaceholderI18nId:"page.businesses.search-placeholder",pageHeadContent:he,dataFetcher:W.Fj,searchItemFilter:Ce,renderItem:fe}),(0,t.jsx)(k.Z,{width:Z,title:o.title,footer:null,open:n,onCancel:Se,children:o.content})]})},ie=ae},55394:function(ce,D,e){e.d(D,{t:function(){return R}});var z=e(5574),M=e.n(z);function R(m,w){var W=window.location.pathname,H=W.split("/"),B=H.slice(0,-1);B.push(m);var U=new URLSearchParams;Object.entries(w).forEach(function(Y){var k=M()(Y,2),T=k[0],q=k[1];U.set(T,q)});var G=U.toString(),f=B.join("/");return"".concat(f,"?").concat(G)}},10305:function(ce,D,e){e.d(D,{t:function(){return z}});var z=function(R){var m=R.split(".");return m.length>0?m[m.length-1]:""}}}]);
