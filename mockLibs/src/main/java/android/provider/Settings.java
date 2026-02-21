package android.provider;

import android.content.ContentResolver;

public class Settings {
    public static final class Secure {
        public static final String USER_SETUP_COMPLETE = "user_setup_complete";

        public static int getInt(ContentResolver cr, String name, int def) {
            return 0;
        }

        public static boolean putInt(ContentResolver cr, String name, int value) {
            return false;
        }
    }
}
