import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
M = """BUCARAMANGA,Bucaramanga,7.1193,-73.1227,1,3
FLORIDABLANCA,Floridablanca,7.0622,-73.0864,1,2
PIEDECUESTA,Piedecuesta,6.9878,-73.0497,1,1
GIRON,Girón,7.0682,-73.1698,1,1
LEBRIJA,Lebrija,7.1136,-73.2181,1,1
LOS_SANTOS,Los Santos,6.7556,-73.1033,1,1
PESCADERO,Pescadero,6.8300,-73.0250,0,0
PARQUE_CHICAMOCHA,Parque Chicamocha,6.7800,-73.0150,0,0
ARATOCA,Aratoca,6.6947,-73.0180,1,1
CEPITA,Cepitá,6.7536,-72.9744,1,1
CURITI,Curití,6.6056,-73.0686,1,1
VILLANUEVA,Villanueva,6.6717,-73.1747,1,1
BARICHARA,Barichara,6.6353,-73.2233,1,1
ZAPATOCA,Zapatoca,6.8153,-73.2689,1,1
SAN_GIL,San Gil,6.5553,-73.1339,1,2
PINCHOTE,Pinchote,6.5311,-73.1747,0,0
SOCORRO,Socorro,6.4681,-73.2603,1,2
VALLE_SAN_JOSE,Valle de San José,6.4472,-73.1439,1,1
PARAMO,Páramo,6.4164,-73.1717,1,1
MOGOTES,Mogotes,6.4758,-72.9703,1,1
CHARALA,Charalá,6.2856,-73.1467,1,1
OIBA,Oiba,6.2647,-73.2992,1,1"""
T = """T01,BUCARAMANGA,FLORIDABLANCA,8,PRINCIPAL
T02,FLORIDABLANCA,PIEDECUESTA,10,PRINCIPAL
T03,BUCARAMANGA,GIRON,9,PRINCIPAL
T04,GIRON,LEBRIJA,17,PRINCIPAL
T05,PIEDECUESTA,PESCADERO,40,PRINCIPAL
T06,PESCADERO,PARQUE_CHICAMOCHA,12,PRINCIPAL
T07,PARQUE_CHICAMOCHA,ARATOCA,10,PRINCIPAL
T08,ARATOCA,CURITI,12,PRINCIPAL
T09,CURITI,SAN_GIL,11,PRINCIPAL
T10,PIEDECUESTA,LOS_SANTOS,33,SECUNDARIA
T11,LOS_SANTOS,VILLANUEVA,48,DESTAPADA
T12,VILLANUEVA,SAN_GIL,22,SECUNDARIA
T13,VILLANUEVA,BARICHARA,15,SECUNDARIA
T14,BARICHARA,SAN_GIL,20,SECUNDARIA
T15,GIRON,ZAPATOCA,55,SECUNDARIA
T16,ZAPATOCA,BARICHARA,45,DESTAPADA
T17,ARATOCA,CEPITA,25,DESTAPADA
T18,SAN_GIL,PINCHOTE,7,PRINCIPAL
T19,PINCHOTE,SOCORRO,16,PRINCIPAL
T20,SOCORRO,OIBA,30,PRINCIPAL
T21,SAN_GIL,VALLE_SAN_JOSE,13,SECUNDARIA
T22,VALLE_SAN_JOSE,PARAMO,6,SECUNDARIA
T23,PARAMO,CHARALA,23,SECUNDARIA
T24,SAN_GIL,MOGOTES,36,SECUNDARIA
T25,PESCADERO,LOS_SANTOS,18,DESTAPADA"""
mun={}
for l in M.splitlines():
    i,n,la,lo,h,nv=l.split(","); mun[i]=(n,float(la),float(lo),int(h),int(nv))
estilo={"PRINCIPAL":("#2E5C7A",2.6,"-"),"SECUNDARIA":("#6B8FA8",1.6,"-"),"DESTAPADA":("#A1887F",1.4,(0,(4,2)))}
fig,ax=plt.subplots(figsize=(7.2,8.6),dpi=200)
for l in T.splitlines():
    t,a,b,km,tipo=l.split(","); c,w,ls=estilo[tipo]
    xa,ya=mun[a][2],mun[a][1]; xb,yb=mun[b][2],mun[b][1]
    if t=="T18": continue_label=True
    if t=="T06":
        ax.plot([xa,xb],[ya,yb],color="#C62828",lw=3.2,zorder=2)
        ax.text(xb+0.004,6.809,"T06: cierre en la demo",fontsize=7,color="#C62828",va="center",ha="left")
    else:
        ax.plot([xa,xb],[ya,yb],color=c,lw=w,linestyle=ls,zorder=1)
        if t!="T18": ax.text((xa+xb)/2,(ya+yb)/2,f"{t}",fontsize=5.5,color="#546E7A",ha="center",va="center",
                bbox=dict(boxstyle="round,pad=0.12",fc="white",ec="none",alpha=0.85),zorder=3)
for i,(n,la,lo,h,nv) in mun.items():
    col = "#C62828" if nv==3 else ("#E08E0B" if nv==2 else ("#2E7D32" if h else "#90A4AE"))
    s = 90 if nv==3 else (60 if nv==2 else 34)
    ax.scatter(lo,la,s=s,c=col,edgecolors="white",linewidths=0.8,zorder=4)
    if i=="PINCHOTE": ax.text(lo-0.008,la-0.012,n,fontsize=7.2,zorder=5,ha="right")
    else: ax.text(lo+0.008,la+0.008,n,fontsize=7.2,zorder=5)
from matplotlib.lines import Line2D
leg=[Line2D([0],[0],color="#2E5C7A",lw=2.6,label="Vía principal (60 km/h)"),
     Line2D([0],[0],color="#6B8FA8",lw=1.6,label="Vía secundaria (40 km/h)"),
     Line2D([0],[0],color="#A1887F",lw=1.4,ls=(0,(4,2)),label="Vía destapada (25 km/h)"),
     Line2D([0],[0],marker="o",color="w",markerfacecolor="#C62828",ms=8,label="Hospital nivel 3"),
     Line2D([0],[0],marker="o",color="w",markerfacecolor="#E08E0B",ms=7,label="Hospital nivel 2"),
     Line2D([0],[0],marker="o",color="w",markerfacecolor="#2E7D32",ms=6,label="Hospital nivel 1"),
     Line2D([0],[0],marker="o",color="w",markerfacecolor="#90A4AE",ms=6,label="Sin hospital")]
ax.legend(handles=leg,loc="upper center",bbox_to_anchor=(0.5,-0.06),ncol=2,fontsize=7,frameon=False)
ax.set_xlabel("Longitud",fontsize=8); ax.set_ylabel("Latitud",fontsize=8)
ax.tick_params(labelsize=7); ax.set_aspect("equal"); ax.grid(alpha=0.15)
for s in ["top","right"]: ax.spines[s].set_visible(False)
plt.tight_layout(); plt.savefig("00-red-vial.png",bbox_inches="tight",dpi=400)
