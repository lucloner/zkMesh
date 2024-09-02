package net.vicp.biggee.bundle;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
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

    public void testLookback() throws UnknownHostException {
        assertTrue(InetAddress.getByName("127.0.0.1").isLoopbackAddress());
        assertTrue(InetAddress.getByName("localhost").isLoopbackAddress());
        assertFalse(InetAddress.getByName("192.168.4.251").isLoopbackAddress());
        assertTrue(InetAddress.getByName("127.0.0.2").isLoopbackAddress());
    }
}
