# 保持 Room 生成的实现类
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# MPAndroidChart 反射相关
-keep class com.github.mikephil.charting.** { *; }
