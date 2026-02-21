package android.app;

import android.content.res.Configuration;

public class ActivityManager {
    public static IActivityManager getService() {
        return null;
    }

    public interface IActivityManager {
        Configuration getConfiguration() throws android.os.RemoteException;
    }
}
