package edu.rice.seclab.keyexporter;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("edu.rice.seclab.keyexporter.test", appContext.getPackageName());
    }

    @Test
    public void scryptTest1(){

        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();


        byte [] salt = new byte [] {'1','1','1','1'};
        ScryptKeyExporter keyExporter = new  ScryptKeyExporter(salt, 16384, 8, 1, 64);
        EditText tv = new EditText(appContext);
        tv.setText("1111");
        keyExporter.bind(tv);
        keyExporter.init();
        String key = keyExporter.getKey();

        String answer = "183D1400AD6366F899E060C3456E7f58d9b244a644e516104aeb4bc7b9fee953f70cae1bf1ee41423c8f180e0556bdde11f8f2ccb0cdd5a5411384003f06dc17".toUpperCase();

        assertEquals(answer, key);
    }

    @Test
    public void scryptTest2(){

        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();


        byte [] salt = new byte [] {'1','2','1','2','1','2','1','2','1','2','1','2'};
        ScryptKeyExporter keyExporter = new  ScryptKeyExporter(salt, 16384, 8, 1, 64);
        EditText tv = new EditText(appContext);
        tv.setText("asdfasdfasdfasdf");
        keyExporter.bind(tv);
        keyExporter.init();
        String key = keyExporter.getKey();

        String answer = "fae66c4d6c7548b1754ef40d39b8bb2903b336568bba715985934c9731b74a23583310d86fafcbd7b79e83f33a71d2081befb0c219e36a1e673dcbf76c6790aa".toUpperCase();

        assertEquals(answer, key);
    }
}
