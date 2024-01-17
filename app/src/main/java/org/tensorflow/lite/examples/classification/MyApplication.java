package org.tensorflow.lite.examples.classification;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;


public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ACRA.init(this, new CoreConfigurationBuilder()
                .withBuildConfigClass(BuildConfig.class)
                .withReportFormat(StringFormat.JSON)
                .withPluginConfigurations(
                        new MailSenderConfigurationBuilder()
                                .withMailTo("lorenzo.massa@unifi.it")
                                .withSubject("[Smart Tourism] ACRA Crash Report")
                                .withReportFileName("crash_report.txt")
                                .build()
                )
        );
    }
}
