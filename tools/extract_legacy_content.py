import re, json, os, shutil, sqlite3
SRC='/home/claude/src/kidsgame/app/src/main'
OUT='/home/claude/out/kidsgame-kmp'
RES=f'{OUT}/composeApp/src/commonMain/composeResources'
os.makedirs(f'{RES}/drawable',exist_ok=True); os.makedirs(f'{RES}/files',exist_ok=True); os.makedirs(f'{RES}/font',exist_ok=True)

# ---- 1. learning content from SubActivity.java
src=open(f'{SRC}/java/com/kidsplay/kidseducation/activity/SubActivity.java',encoding='utf8').read()
body=src[src.index('prepareDataForLearning(int i2)'):]
parts=re.split(r'(?:else )?if \(i2 == (\d+)\) \{',body)
# parts: [pre, idx, block, idx, block...]; last block contains trailing else
pat=re.compile(r'new LearningDataModel\(R\.drawable\.([a-z0-9_]+),\s*("(?:[^"\\]|\\.)*"|ExifInterface\.TAG_MODEL),\s*("(?:[^"\\]|\\.)*"|ExifInterface\.TAG_MODEL)\)')
def val(s): return 'Model' if s.startswith('Exif') else json.loads(s)
cats={}
for i in range(1,len(parts),2):
    idx=int(parts[i]); blk=parts[i+1]
    if idx==21: blk=blk.split('} else {')[0]
    if idx==1:
        hi_part,en_part=blk.split('} else {',1)
        hi=[(m.group(1),val(m.group(2)),val(m.group(3))) for m in pat.finditer(hi_part)]
        en=[(m.group(1),val(m.group(2)),val(m.group(3))) for m in pat.finditer(en_part)]
        items=[{'image':e[0],'speak':e[1],'show':e[2],'speakHi':h[1]} for e,h in zip(en,hi)]
        assert len(hi)==len(en),(len(hi),len(en))
    else:
        items=[{'image':m.group(1),'speak':val(m.group(2)),'show':val(m.group(3))} for m in pat.finditer(blk)]
    cats[idx]=items
titles=["Alphabets","Numbers","Colors","Shapes","Animals","Birds","Flowers","Fruits","Month","Vegetable","Body Parts","Clothes","Country","Food","Geometry","House","Jobs","School","Sports","Vehicle","Golden Words","Lines"]
icons=["home_alphabet","home_number","home_color","home_shape","home_animal","home_birds","home_flower","home_fruits","home_month","home_vegetable","home_body_parts","home_clothes","home_country","home_food","home_geometry","home_house","home_jobs","home_school","home_sports","home_vehicle","home_golden","line_zigzag"]
out=[{'index':i,'title':titles[i],'icon':icons[i],'items':cats[i]} for i in range(22)]
json.dump({'version':1,'categories':out},open(f'{RES}/files/learning.json','w',encoding='utf8'),ensure_ascii=False,indent=1)
print('categories',{i:len(cats[i]) for i in sorted(cats)})

# ---- 2. videos from sqlite
c=sqlite3.connect(f'{SRC}/assets/databases/preschool.db')
names=["ABC Songs","Number Songs","Color Songs","Animal Songs","Shape Songs","Vehicle Songs","Fruit Songs","Vegetable Songs","Day Songs","Month Songs","Clothes Songs","Rhymes"]
vc=[]
for i,n in enumerate(names):
    vids=[]
    for (d,) in c.execute('select data from kids where id=?',(str(i),)):
        p=d.split('#',1)
        if len(p)>=2: vids.append({'videoId':p[0],'title':p[1]})
    vc.append({'id':i,'title':n,'videos':vids})
json.dump({'version':1,'categories':vc},open(f'{RES}/files/videos.json','w',encoding='utf8'),ensure_ascii=False,indent=1)
print('videos',sum(len(x['videos']) for x in vc))

# ---- 3. life skills json
shutil.copy(f'{SRC}/assets/life_skills_questions.json',f'{RES}/files/life_skills_questions.json')

# ---- 4. fonts
shutil.copy(f'{SRC}/assets/Font/OhWhale.ttf',f'{RES}/font/ohwhale.ttf')
shutil.copy(f'{SRC}/res/font/poppins_bold.ttf',f'{RES}/font/poppins_bold.ttf')

# ---- 5. drawables: raster + vector xml (not shapes/selectors)
n=0; used=set()
for f in os.listdir(f'{SRC}/res/drawable'):
    p=f'{SRC}/res/drawable/{f}'
    if f.endswith(('.png','.webp','.jpg')):
        shutil.copy(p,f'{RES}/drawable/{f}'); n+=1
    elif f.endswith('.xml'):
        t=open(p,encoding='utf8').read()
        if re.search(r'<vector\b',t) and not f.startswith(('ic_launcher',)):
            shutil.copy(p,f'{RES}/drawable/{f}'); n+=1
print('drawables copied',n)
missing=[it['image'] for c_ in out for it in c_['items'] if not any(os.path.exists(f'{RES}/drawable/{it["image"]}.{e}') for e in ('png','webp','jpg','xml'))]
miss_icons=[i for i in icons if not any(os.path.exists(f'{RES}/drawable/{i}.{e}') for e in ('png','webp','jpg','xml'))]
print('missing item images',missing,'missing icons',miss_icons)
