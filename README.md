# V2Ray Client (Android)

کلاینت اندروید V2Ray/Xray با معماری MVVM، Jetpack Compose، و تم دارک شیشه‌ای (Glassmorphism).

## چی کار می‌کنه (کامل و آماده)
- مدیریت چند سرور: افزودن، حذف، علامت‌گذاری علاقه‌مندی، تست پینگ (TCP handshake)
- Import لینک اشتراک‌گذاری: `vless://` ، `vmess://` ، `trojan://` ، `ss://`
- اسکن QR با دوربین (CameraX + ML Kit) برای وارد کردن سریع سرور
- فرم افزودن دستی سرور با همه‌ی فیلدهای رایج (network, path, host, TLS, SNI, Reality: pbk/sid)
- ساخت خودکار JSON کانفیگ کامل Xray-core از روی مشخصات سرور (`XrayConfigBuilder`)
- ذخیره‌سازی دائمی با Room + تنظیمات با DataStore
- صفحه‌ی آمار ترافیک (آپلود/دانلود/مدت اتصال) و لاگ هسته
- ساختار VpnService با نوتیفیکیشن Foreground و درخواست مجوز VPN از سیستم

## قدم‌به‌قدم: وصل کردن هسته‌ی واقعی Xray

API واقعی کتابخانه‌ی [AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite) رو
بررسی کردم؛ خبر خوب اینه که نسخه‌ی فعلی خیلی ساده‌تره — دیگه نیازی به tun2socks
جدا نیست، چون `StartLoop` مستقیماً فایل‌دیسکریپتور TUN رو می‌گیره:

```go
type CoreCallbackHandler interface {
    Startup() int
    Shutdown() int
    OnEmitStatus(int, string) int
}
func NewCoreController(s CoreCallbackHandler) *CoreController
func (x *CoreController) StartLoop(configContent string, tunFd int32) (err error)
func (x *CoreController) StopLoop() error
func (x *CoreController) QueryStats(tag string, direct string) int64
```

کدهای پروژه (`CoreController.kt` و `V2RayVpnService.kt`) از قبل با این امضای دقیق
هماهنگ شدن و فقط چند خط کامنت‌شده لازم دارن باز بشن. مراحل:

> **راه ساده‌تر:** به‌جای انجام دستی مراحل ۱ تا ۴، می‌تونی از
> `.github/workflows/build.yml` استفاده کنی — این workflow خودش AAR رو با
> gomobile می‌سازه و APK نهایی رو بهت می‌ده، بدون نیاز به نصب چیزی روی سیستم
> خودت. جزئیات در بخش «بیلد خودکار با GitHub Actions» پایین همین فایل.

### ۱) نصب پیش‌نیازها (روی سیستم خودت — لینوکس/مک/ویندوز، نه توی اندروید استودیو)
```bash
# نصب Go (نسخه‌ی 1.21 یا بالاتر)
# نصب JDK 17 و Android SDK (از طریق Android Studio)
go install golang.org/x/mobile/cmd/gomobile@latest
export PATH=$PATH:$(go env GOPATH)/bin
gomobile init
```

### ۲) کلون و بیلد AAR
```bash
git clone https://github.com/2dust/AndroidLibXrayLite
cd AndroidLibXrayLite
go mod tidy -v
gomobile bind -v -androidapi 21 -ldflags='-s -w -checklinkname=0' -o libv2ray.aar ./
```
> فلگ `-checklinkname=0` لازمه چون Go 1.23+ یه linkname check سخت‌گیرانه‌تر داره که
> با وابستگی `wlynxg/anet` (استفاده‌شده برای گرفتن لیست اینترفیس‌های شبکه روی
> اندروید) تداخل داره و خطای `invalid reference to net.zoneCache` می‌ده.
بعد از چند دقیقه یک فایل `libv2ray.aar` تولید می‌شه.

### ۳) اضافه کردن AAR به پروژه
```bash
cp /مسیر/به/libv2ray.aar app/libs/
```
وابستگی گریدل از قبل فعاله (`implementation(files("libs/libv2ray.aar"))`)، فقط
فایل باید سر جاش باشه. **بدون این فایل، پروژه اصلاً کامپایل نمی‌شه** چون
`CoreController.kt` مستقیم از `libv2ray` import می‌کنه.

### ۴) تست
1. Gradle sync کن.
2. اپ رو روی گوشی/امولاتور نصب کن (`minSdk=24` پس امولاتور با API 24+ هم جواب می‌ده).
3. یه سرور اضافه کن (لینک یا QR)، انتخابش کن، دکمه‌ی وسط رو بزن.
4. مجوز VPN که سیستم می‌پرسه رو تأیید کن.
5. توی صفحه‌ی «آمار» باید ببینی uplink/downlink بالا میره و در صفحه‌ی اصلی وضعیت
   «متصل» نشون داده بشه.

اگه به مشکلی خوردی (مثلاً `UnsatisfiedLinkError` یا کرش موقع `StartLoop`)، معمولاً
یعنی نسخه‌ی `androidapi` که با gomobile ست کردی با `minSdk` پروژه هم‌خوانی نداره —
هر دو رو باید حداقل ۲۱ بذاری (توی این پروژه ۲۴ هست که مشکلی نداره).


## ساختار پروژه
```
app/src/main/java/com/mipycode/v2rayclient/
├── data/
│   ├── model/ServerConfig.kt       # مدل و Entity سرور
│   ├── db/                         # Room (Dao + Database)
│   ├── parser/LinkParser.kt        # پارس لینک vless/vmess/trojan/ss
│   └── repository/ServerRepository.kt
├── service/
│   ├── V2RayVpnService.kt          # سرویس VPN (TUN interface)
│   ├── CoreController.kt           # واسط اتصال به هسته‌ی نیتیو (نیاز به تکمیل)
│   └── VpnStatus.kt                # وضعیت سراسری برای UI
├── ui/
│   ├── servers/                    # صفحه‌ی اصلی لیست سرورها
│   ├── addserver/                  # افزودن (لینک/QR/دستی)
│   ├── stats/                      # آمار ترافیک
│   ├── settings/                   # تنظیمات (DataStore)
│   └── theme/                      # تم دارک + GlassCard + فونت وزیرمتن
└── util/XrayConfigBuilder.kt       # ساخت JSON کانفیگ Xray-core
```

## بیلد گرفتن
1. پروژه رو در Android Studio (Koala یا جدیدتر) باز کن، بذار Gradle sync بشه.
2. اگه هسته رو طبق بالا اضافه نکرده باشی، اپ بیلد و نصب می‌شه ولی دکمه‌ی
   اتصال فقط پیام Log می‌ده و واقعاً تانل نمی‌زنه (رفتار امن، بدون کرش).
3. `minSdk = 24`, `compileSdk = targetSdk = 34`.
4. برای Release، کلید امضا (`signingConfig`) رو خودت طبق روال معمول اضافه کن.

## فونت
فایل‌های Vazirmatn (Regular/Medium/Bold) از قبل در `res/font/` قرار داده شدن.

## نکته‌ی امنیتی
لینک‌های سرور و UUID/پسورد به‌صورت متن ساده در دیتابیس Room ذخیره می‌شن (مثل
اغلب کلاینت‌های مشابه). اگه نیاز به رمزنگاری در سطح دیتابیس داری،
`SQLCipher` رو جایگزین Room معمولی کن.

## بیلد خودکار با GitHub Actions

فایل `.github/workflows/build.yml` از قبل توی پروژه هست. مراحل استفاده:

1. یه ریپوی جدید و **خالی** توی گیت‌هاب بساز (بدون README اولیه، بدون .gitignore).
2. توی پوشه‌ی پروژه:
   ```bash
   cd V2RayClient
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/USERNAME/REPO.git
   git push -u origin main
   ```
3. برو توی تب **Actions** ریپو روی گیت‌هاب — workflow به‌محض push خودکار اجرا
   می‌شه (چون `on: push: branches: [main]` تنظیم شده). اگه اجرا نشد، از همون
   تب Actions دستی روی «Run workflow» بزن (چون `workflow_dispatch` هم فعاله).
4. بیلد حدود ۱۰-۱۵ دقیقه طول می‌کشه (بیشترش برای کامپایل هسته‌ی Go هست).
5. وقتی سبز شد، وارد اون run بشو، پایین صفحه بخش **Artifacts** رو ببین —
   یه فایل zip به اسم `v2ray-client-debug-apk` اونجاست که APK نصب‌شدنی توشه.

### نکات مهم
- این APK **بدون امضا (unsigned/debug)** ساخته می‌شه؛ برای نصب روی گوشی خودت
  کافیه (اندروید اجازه‌ی نصب APKهای debug رو با فعال کردن «منابع ناشناس» می‌ده)،
  ولی برای انتشار در بازار/Google Play باید امضاش کنی.
- برای امضای Release، باید یه keystore بسازی و به‌صورت GitHub Secret
  (`Settings → Secrets and variables → Actions`) اضافه‌ش کنی، بعد یه Job جدا
  توی workflow برای `assembleRelease` با اون امضا بنویسی — بگو اگه می‌خوای
  این بخش رو هم برات آماده کنم.
- اگه ریپو رو Private کردی، همچنان Actions به‌صورت رایگان (با محدودیت دقیقه‌ی
  ماهانه) روش کار می‌کنه.
