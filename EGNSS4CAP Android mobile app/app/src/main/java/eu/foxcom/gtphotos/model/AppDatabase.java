package eu.foxcom.gtphotos.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import eu.foxcom.gtphotos.model.pathTrack.PTDao;
import eu.foxcom.gtphotos.model.pathTrack.PTPath;
import eu.foxcom.gtphotos.model.pathTrack.PTPoint;

@Database(entities = {LoggedUser.class, Task.class, Photo.class, PTPath.class, PTPoint.class}, version = AppDatabase.VERSION)
@TypeConverters({Convertes.class})
public abstract class AppDatabase extends RoomDatabase {

    public static final Migration MIGRATION_42_43 = new Migration(42, 43) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("alter table Photo add isLastSendFailed INTEGER not null default 0");
        }
    };

    public static final Migration MIGRATION_43_44 = new Migration(43, 44) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("alter table Task add isLastSendFailed INTEGER not null default 0");
        }
    };

    public static final Migration MIGRATION_44_45 = new Migration(44, 45) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("alter table Photo add tilt REAL default null");
        }
    };

    public static final Migration MIGRATION_45_46 = new Migration(45, 46) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `PTPath` (`autoId` INTEGER PRIMARY KEY AUTOINCREMENT, `realId` INTEGER,  `userId` TEXT NOT NULL, `name` TEXT, `startT` INTEGER, `endT` INTEGER)");
            database.execSQL("CREATE UNIQUE INDEX `index_PTPath_realId` ON `PTPath` (`realId`)");
            database.execSQL("CREATE TABLE IF NOT EXISTS `PTPoint` (`autoId` INTEGER PRIMARY KEY AUTOINCREMENT, `pathId` INTEGER NOT NULL, `index` INTEGER, `latitude` REAL, `longitude` REAL, `created` INTEGER, FOREIGN KEY(`pathId`) REFERENCES `PTPath`(`autoId`) ON UPDATE CASCADE ON DELETE CASCADE )");
            database.execSQL("CREATE INDEX `index_PTPoint_pathId` ON `PTPoint` (`pathId`)");
        }
    };

    public static final Migration MIGRATION_46_47 = new Migration(46, 47) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("alter table PTPath add byCentroids integer not null default 0");
            database.execSQL("alter table PTPath add area real");
        }
    };

    public static final int VERSION = 47;

    public static AppDatabase build(Context applicationContext) {
        return Room.databaseBuilder(applicationContext, AppDatabase.class, "database-name").allowMainThreadQueries().
                addMigrations(MIGRATION_42_43, MIGRATION_43_44, MIGRATION_44_45, MIGRATION_45_46, MIGRATION_46_47)
                .build();
    }

    public abstract LoggedUserDao loggedUserDao();

    public abstract TaskDao taskDao();

    public abstract PhotoDao photoDao();

    public abstract PTDao PTDao();
}
