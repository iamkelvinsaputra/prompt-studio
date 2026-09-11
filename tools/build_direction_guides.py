"""Rebuild the replaceable lighting/environment diagrams; UI only reads registry assets."""
from pathlib import Path
import json, math
from PIL import Image, ImageDraw, ImageFilter

root = Path(__file__).resolve().parents[1] / 'shared/src/commonMain/composeResources/files/visual-guides'
registry = json.loads((root / 'registry.json').read_text())
W,H=320,400
paper='#F2EEE7'; ink='#55596B'; accent='#D4B56F'
directions=[('front','Front',160,320),('front-left','Front-left',52,288),('front-right','Front-right',268,288),('left','Left side',30,170),('right','Right side',290,170),('back','Back',160,38),('back-left','Back-left',55,55),('back-right','Back-right',265,55),('top','Top',160,28),('below','Below',160,362)]
for order,(id,label,lx,ly) in enumerate(directions):
 im=Image.new('RGB',(W,H),paper); d=ImageDraw.Draw(im)
 d.ellipse((45,266,275,318),outline='#CEC8BE',width=2)
 # Light beam sits behind the figure; source remains visible.
 d.polygon([(lx,ly),(110,160),(210,250)], fill='#E7DDC4')
 mask=Image.new('L',(W,H)); m=ImageDraw.Draw(mask)
 m.ellipse((133,89,187,148),fill=255)
 m.rounded_rectangle((110,151,210,248),radius=34,fill=255)
 m.line((130,224,124,298),fill=255,width=24);m.line((190,224,196,298),fill=255,width=24)
 m.line((114,170,91,237),fill=255,width=17);m.line((206,170,229,237),fill=255,width=17)
 fill=Image.new('RGB',(W,H)); pix=fill.load()
 for y in range(H):
  for x in range(W):
   dist=math.hypot((x-lx)/1.3,y-ly)
   bright=max(0,min(1,1-dist/280))
   if id=='front': bright=.83
   if id=='front-left': bright=max(.1,min(1,1-(x-90)/150))
   if id=='front-right': bright=max(.1,min(1,(x-90)/150))
   if id.startswith('back'): bright=max(0,min(1,(abs(x-160)-25)/32))*.8
   pix[x,y]=tuple(int(a+(b-a)*bright) for a,b in zip((67,73,91),(227,205,147)))
 im.paste(fill,(0,0),mask);d=ImageDraw.Draw(im)
 d.ellipse((lx-13,ly-13,lx+13,ly+13),fill=accent)
 for a in range(0,360,45):
  r=math.radians(a); d.line((lx+18*math.cos(r),ly+18*math.sin(r),lx+24*math.cos(r),ly+24*math.sin(r)),fill=accent,width=2)
 tx,ty=160,200; vx,vy=tx-lx,ty-ly; norm=math.hypot(vx,vy); vx/=norm;vy/=norm
 x1,y1=lx+31*vx,ly+31*vy;x2,y2=lx+62*vx,ly+62*vy
 d.line((x1,y1,x2,y2),fill='#A88947',width=3)
 d.polygon([(x2,y2),(x2-10*vx+5*vy,y2-10*vy-5*vx),(x2-10*vx-5*vy,y2-10*vy+5*vx)],fill='#A88947')
 if id=='front': desc='Light faces the character; face and chest are bright.'
 elif id.startswith('back'): desc='Light behind the character outlines the silhouette.'
 elif id=='top': desc='Light overhead brightens the head and shoulders.'
 elif id=='below': desc='Light below brightens the chin and lower surfaces.'
 else: desc=f'Light from {label.lower()} brightens that side of the figure.'
 path=f'lighting-direction/{id}.png';(root/path).parent.mkdir(exist_ok=True);im.save(root/path)
 registry[f'lighting-direction/{id}']={'image':path,'description':desc,'order':order,'tags':['light','direction',label.lower()]}

for order,(id,label) in enumerate([('none','No environment'),('minimal','Minimal'),('studio','Studio'),('interior','Interior'),('urban','Urban'),('nature','Nature'),('fantasy','Fantasy'),('sci-fi','Sci-fi'),('abstract','Abstract')]):
 im=Image.new('RGB',(W,H),paper);d=ImageDraw.Draw(im)
 if id=='none':
  d.rounded_rectangle((44,60,276,335),radius=12,outline='#D7D1C6',width=2)
 elif id=='minimal':
  for y in range(70,330):
   c=int(240-(y-70)/13);d.line((30,y,290,y),fill=(c,c,c+1))
 elif id=='studio':
  d.rounded_rectangle((45,50,275,335),radius=70,fill='#E0D9CC');d.line((65,270,255,270),fill='#C3B9A9',width=3)
 elif id=='interior':
  d.rectangle((35,65,285,315),fill='#E2D9CE');d.rectangle((65,92,150,200),fill='#A7BFC6');d.line((107,92,107,200),fill=paper,width=4);d.line((65,146,150,146),fill=paper,width=4)
  d.rectangle((181,100,259,265),fill='#AAA093')
  for y in [125,168,212]: d.line((185,y,255,y),fill=paper,width=4)
 elif id=='urban':
  for x,y,w in [(25,115,55),(90,70,70),(172,100,60),(244,145,50)]:
   d.rectangle((x,y,x+w,280),fill='#A9ABB1')
   for yy in range(y+15,250,28):d.rectangle((x+12,yy,x+w-12,yy+8),fill='#DDD8C9')
  d.polygon([(0,340),(135,245),(190,245),(320,340)],fill='#C8BDB0')
 elif id=='nature':
  d.ellipse((227,64,263,100),fill=accent);d.polygon([(15,250),(95,117),(167,250)],fill='#A3B4A3');d.polygon([(106,250),(230,91),(310,250)],fill='#BAC6BC')
  for x in [50,255]:d.rectangle((x,203,x+8,300),fill='#A59179');d.ellipse((x-24,150,x+32,225),fill='#798C7E')
 elif id=='fantasy':
  for x,y in [(67,145),(135,95),(205,145)]:d.rectangle((x,y,x+45,265),fill='#B7A8BE');d.polygon([(x-6,y),(x+22,y-52),(x+51,y)],fill='#887D97')
  d.arc((42,63,282,308),180,355,fill=accent,width=4)
 elif id=='sci-fi':
  d.ellipse((45,58,275,122),outline='#929CAC',width=8);d.line((45,91,45,287),fill='#929CAC',width=8);d.line((275,91,275,287),fill='#929CAC',width=8)
  d.polygon([(65,292),(119,132),(204,132),(255,292)],fill='#B8C4CF');d.line((160,136,160,276),fill='#F7F2DE',width=4)
 else:
  d.ellipse((25,67,235,278),fill='#B4B1C8');d.polygon([(93,285),(232,72),(289,302)],fill='#CEB992')
 # A constant small human gives every environment a comparable scale.
 d.ellipse((147,226,173,254),fill=ink);d.rounded_rectangle((141,255,179,301),radius=12,fill=ink);d.line((150,290,148,331),fill=ink,width=9);d.line((170,290,173,331),fill=ink,width=9)
 path=f'environment/{id}.png';(root/path).parent.mkdir(exist_ok=True);im.save(root/path)
 registry[f'environment/{id}']={'image':path,'description':f'{label} setting; refine the place after selecting.','order':order}
# Side-view camera-position diagrams explain angle independently of framing.
for order,(id,label,cx,cy) in enumerate([('eye-level','Eye level',53,122),('low','Low angle',52,277),('high','High angle',52,60),('overhead','Overhead',211,36),('dutch','Dutch angle',53,122)]):
 im=Image.new('RGB',(W,H),paper);d=ImageDraw.Draw(im)
 d.line((24,323,296,323),fill='#CCC5B9',width=3)
 d.ellipse((190,95,232,142),fill=ink);d.polygon([(227,113),(241,125),(224,129)],fill=ink)
 d.rounded_rectangle((188,148,232,250),radius=19,fill=ink)
 d.line((200,237,191,321),fill=ink,width=15);d.line((220,237,235,321),fill=ink,width=15)
 d.line((192,165,171,231),fill=ink,width=10)
 target=(211,120) if id=='eye-level' else (211,181)
 d.polygon([(cx+14,cy),(195,target[1]-32),(228,target[1]+32)],fill='#DED6C1')
 d.line((cx+12,cy,*target),fill='#AA8D56',width=3)
 # Camera body and lens, oriented toward the character.
 camera=Image.new('RGBA',(64,52));c=ImageDraw.Draw(camera)
 c.rounded_rectangle((5,12,42,43),radius=5,fill='#8A705B');c.polygon([(42,21),(58,12),(58,43),(42,34)],fill='#8A705B');c.rectangle((14,5,31,14),fill='#8A705B')
 angle=-math.degrees(math.atan2(target[1]-cy,target[0]-cx))
 camera=camera.rotate(angle,expand=True,resample=Image.Resampling.BICUBIC)
 im.paste(camera,(int(cx-camera.width/2),int(cy-camera.height/2)),camera)
 if id=='dutch':
  # Explicit tilted view frame; same eye height, different camera roll.
  d=ImageDraw.Draw(im);d.polygon([(105,72),(276,104),(236,317),(65,285)],outline='#AA8D56',width=4)
 path=f'camera-angle/{id}.png';(root/path).parent.mkdir(exist_ok=True);im.save(root/path)
 descriptions=['Camera meets the character at eye height.','Camera below the character looks upward.','Camera above the character looks downward.','Camera directly overhead looks straight down.','A tilted frame creates a diagonal horizon.']
 registry[f'camera-angle/{id}']={'image':path,'description':descriptions[order],'order':order}
(root/'registry.json').write_text(json.dumps(registry,indent=4)+'\n')
