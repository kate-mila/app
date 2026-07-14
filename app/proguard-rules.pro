# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# MLKit barcode
-keep class com.google.mlkit.vision.barcode.** { *; }

# اگر AndroidLibXrayLite اضافه شد، طبق مستندات آن قوانین keep مربوطه را هم اضافه کن.
