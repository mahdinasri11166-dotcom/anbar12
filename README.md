# کالانو (Kalano) v4.6.0

مدیریت انبار و فروش — سازنده: مهدی طریری · mahditariri@gmail.com

## ساخت APK در GitHub Actions

1. زیپ را باز کنید
2. **محتویات داخل پوشه AnbarApp** را در ریشه ریپو قرار دهید:
   - `.github/workflows/build.yml`
   - `app/`
   - `gradle/wrapper/`
   - `gradlew`
   - `build.gradle.kts`
   - ...
3. Push روی `main`
4. Actions → artifact به نام **app-debug**

```
./gradlew :app:assembleDebug
```

حداقل اندروید ۱۰ (API 29)
