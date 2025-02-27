package xyz.maphaze;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import xyz.maphaze.utils.MinioConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    public void testMinioConfig() {

        MinioConfig minioConfig = new MinioConfig();
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");

        MinioConfig.kitSetFilenames.put("key",list);

        minioConfig.saveKitSetFilename();


    }



    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
}
