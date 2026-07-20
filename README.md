# Auto Builder (Fabric mod moi cho Minecraft 1.21.4)

Mod client hoan toan moi, tach rieng khoi Auto Flyer: tu dong **lay nguyen lieu tu ruong/thung trong
1 vung ban chon**, roi **tu bay den va tu dat block** de xay het ban ve Litematica dang tai (dung tinh
nang "Easy Place" cua Litematica de canh chinh huong block).

## Mo GUI
Bam phim **NUM LOCK** (mac dinh) de mo/dong GUI. Doi phim trong **Options -> Controls -> muc "Auto Builder"**.

Luu y: giong moi Screen khac trong Minecraft, khi GUI dang mo thi nhan vat khong the di chuyen -
day la gioi han co san cua Minecraft (ap dung cho moi Screen), khong rieng gi mod nay. Dong GUI
lai (bam Num Lock lan nua) truoc khi bay/lam viec.

## GUI gom
- **Vi tri 1 / Vi tri 2** (x,y,z + nut "Set here" + "Clear" o ca 2): 2 goc doi dien cua 1 khoi hop la
  **vung chua ruong/thung/barrel...** de mod tu bay toi lay nguyen lieu.
- **Toc do bay**.
- **Che do phat hien nguoi choi khac** (bam de doi vong qua 3 che do):
  1. **Tat**: khong lam gi.
  2. **Bat (tu thoat game)**: neu co nguoi choi khac xuat hien gan (trong pham vi client dang tai/hien
     thi), tu dong disconnect ngay lap tuc.
  3. **Dung lai + am thanh**: tam dung Auto Builder, phat am thanh + chat canh bao, nhac lai moi 3
     giay neu nguoi choi van con o gan, cho ban tu quyet dinh.
- **Toc do xay** (so tick giua 2 lan dat block) va **toc do lay do** (so tick giua 2 lan lay item
  trong ruong) - so cang nho thi cang nhanh.
- **Start / Stop**.

## Hoat dong khi bam Start
1. **Tu quet** toan bo block con thieu trong lop (layer) Litematica dang hien - tu tinh so luong
   tung loai vat lieu can dung (khong dua vao Material List co san cua Litematica, tu quet truc tiep
   nhu ban yeu cau). Vi mod luon so sanh THUC TE trong world voi ban ve, **neu ban da xay mot phan
   tu truoc (vd choi tay hoac chay mod nay lan truoc bi dung giua chung), no se TU DONG biet cho nao
   da xong va tiep tuc tu do, khong xay lai tu dau.**
2. Neu trong tui do chua du, **tu bay den tung ruong/thung** trong vung Vi tri 1-2 de lay do:
   - Uu tien den thang cac vi tri **DA BIET tu lan chay truoc** (luu trong `config/autobuilder_chestindex.json`)
     co chua loai vat lieu **dang can NHIEU NHAT truoc**, thay vi di het tung ruong theo thu tu.
   - Ruong nao chua tung mo se duoc mo de kham pha, va **ghi nho lai toan bo loai item thay duoc**
     (ke ca cai khong can lay luc do) vao chi muc, de lan sau nhanh hon.
   - Neu 1 ruong co nhieu loai item can, uu tien lay loai dang thieu nhieu nhat truoc.
3. Sau do **tu bay den va dat tung block**, theo thu tu **tung HANG mot (doc truc Nam-Bac), moi
   hang xay het toan bo truc Y (tu duoi len) roi moi chuyen sang hang tiep theo** - giong cach ban
   mo ta (xay hang 1 xong het truc Y roi moi qua hang 2). Mod tu chon dung item trong hotbar/tui do,
   xoay mat nhin dung huong, roi "click chuot phai" mo phong - neu ban da **bat tinh nang Easy Place
   trong Litematica**, no se tu dieu chinh dung huong/trang thai cho block duoc dat.
4. Neu giua chung het nguyen lieu, tu quay lai buoc 2 lay them roi tiep tuc xay.
5. Xay xong het (khong con block nao thieu trong lop dang hien) -> phat am thanh bao hieu va dung lai.

## Da sua trong ban nay
- **Loi "bay ra xa roi dung im khong lam gi nua"**: nguyen nhan la ca 2 tac vu bay (lay do va xay)
  truoc day KHONG CO gioi han thoi gian nao khi bay toi 1 diem - neu diem do vi ly do gi khong the
  toi duoc (vd tinh toan lech, nam trong khoi dac), no se cho MAI MAI ma khong bao gio bo cuoc. Da
  them **StuckGuard** dung chung: neu khong nhuc nhich duoc ~1.2 giay, tu nhun len va tinh lai duong
  bay; sau 3 lan thu van khong duoc thi tu dong BO QUA vi tri/ruong do va chuyen sang cai khac,
  khong bao gio bi ket vinh vien nua.
- **Chi lay/xay du cho 1 HANG (truc Z) tai 1 thoi diem** thay vi tinh nguyen lieu cho toan bo phan
  con lai cua ban ve - tranh viec vet can het ca ruong trong 1 lan lay.
- **Sua diem dung khi lay do**: truoc kiem tra o cach ruong 2 block nhung lai dung o cach 1.5 block
  - neu ruong ke ben (rat pho bien trong kho do) chan ngay o do, diem dung se de len chinh ruong
    ke ben va gay ket ngay khi vua bay toi. Da sua lai kiem tra DUNG o se dung vao (ca tam chan lan
    tam dau, vi nguoi choi cao ~1.8 block).
- **Loi bi ket ngay sau khi mo ruong roi thoat ra**: them buoc dong chac chan hon (`setScreen(null)`
  du phong) + khoang cho dem sau khi dong.
- **Thu tu xay** doi thanh tung hang (truc Z, Nam -> Bac), het truc Y roi moi qua hang ke.
- **Uu tien lay vat lieu dang thieu nhieu nhat** truoc trong moi ruong.
- **Luu vi tri item da tung thay** vao `config/autobuilder_chestindex.json`, dung lai cho lan sau.
- Da bo nut/thanh trang thai o goc man hinh theo yeu cau truoc - Start/Stop qua GUI (Num Lock).

## Gioi han / dieu can luu y (quan trong, doc truoc khi dung)
Day la tinh nang rat phuc tap, phien ban dau tien nay uu tien "chay duoc" hon la hoan hao tuyet doi;
nhieu kha nang se can ban thu va gui log/mo ta loi de minh chinh tiep, giong nhu cach chung ta da lam
voi Auto Flyer truoc do:
- **Thu tu xay**: trong cung 1 lop Y, mod chon block gan nguoi choi nhat truoc, KHONG dam bao dung
  chinh xac huong "Nam -> Bac" nhu ban mo ta ban dau - vi uu tien dam bao luon co diem tua (Y tang
  dan) quan trong hon huong quet ngang de tranh that bai khi dat block.
- **Lay do tu ruong**: moi lan lay se **lay ca chong (stack)** trong 1 o cua ruong, co the du thua
  hon so voi so luong thuc su can - chap nhan du de don gian hoa, khong lay le tung item mot.
- **Dat block thieu diem tua**: neu 1 vi tri chua co mat nao dac ben canh (vd phan noi/treo lo lung
  trong thiet ke), mod se tam bo qua va thu lai sau khi cac block xung quanh da duoc dat.
- **Khong tu dong ne nguoi choi that ky** - "phat hien nguoi choi" hien dang kiem tra bat ky nguoi
  choi nao ma client dang tai (trong pham vi server gui du lieu ve may ban), khong phan biet duoc
  ban be hay nguoi la.
- Mod dieu khien van toc + tuong tac inventory cua nhan vat - server cua ban da cho phep bay tu do
  (giong Creative) nen phan lon se khong bi anti-cheat chan, nhung hanh vi lay do/dat block nhanh
  bat thuong van co the bi mot so plugin chong hack rieng phat hien.

## Yeu cau
Can 2 mod sau da duoc cai o client (bundle san dung phien ban trong `libs/`):
- **Litematica** (fabric-1.21.4-0.21.7, ban sakura-ryoko fork) - nen **bat tinh nang Easy Place**
  trong cai dat cua Litematica de viec dat block chinh xac huong.
- **malilib** (fabric-1.21.4-0.23.5)

Khong bat buoc phai cai Litematica de mod nay load duoc, nhung Auto Builder se khong hoat dong neu
thieu.

## Cach build bang GitHub Actions (khuyen nghi)
Repo da co san `.github/workflows/build.yml`, tu dong chay tren nhanh `main`/`master`.

```bash
cd autobuilder-mod
git init
git add .
git commit -m "Auto Builder: mod moi hoan toan"
git branch -M main
git remote add origin <link-repo-github-cua-ban>
git push -u origin main
```

Vao tab **Actions** tren GitHub, doi build xong roi tai file trong muc **Artifacts**.

## Cach build tai cho (local)
Can cai **JDK 21** va co ket noi Internet.

```bash
cd autobuilder-mod
gradle wrapper --gradle-version 9.5.1
./gradlew build
```

File jar ket qua nam o `build/libs/autobuilder-1.0.0.jar`.

## Cai dat
1. Cai **Fabric Loader** cho Minecraft 1.21.4.
2. Cai **Fabric API** (bat buoc), **Litematica** + **malilib** (bat buoc de dung duoc).
3. Bo file `autobuilder-1.0.0.jar` vua build vao thu muc `mods`.
4. Trong Litematica, bat **Easy Place** (Configs -> Generic -> Easy Place mode) de dat block dung huong.
5. Load san file schematic ban muon xay trong Litematica (dung nhu ban mo ta - chi can bat file len
   va de do), roi vao game bam Num Lock de mo GUI Auto Builder.
