package org.anc.lapps.ranklib;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.DataSourceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexandru Mahmoud
 */
public class RankLibTest
{
    private RankLib rankLib;

    @Before
    public void setup()
    {
        rankLib = new RankLib();
    }

    @After
    public void cleanup()
    {
        rankLib = null;

    }
    @Test
    public void testMetadata()
    {
        System.out.println("RankLib.testMetadata");
        String json = rankLib.getMetadata();
        DataSourceMetadata metadata = Serializer.parse(json, DataSourceMetadata.class);
        expect("http://www.anc.org", metadata.getVendor());
        expect(Discriminators.Uri.ANY, metadata.getAllow());
        expect(Discriminators.Uri.APACHE2, metadata.getLicense());
        expect("UTF-8", metadata.getEncoding());
        //expect(Version.getVersion(), metadata.getVersion());
    }

    @Test
    public void testExecute()
    {
        System.out.println("RankLib.testExecute");

        //Todo: Input
        Data<String> data = new Data<>(Discriminators.Uri.GET, );

        //
        //data.setParameter("unit", "km");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testErrorInput()
    {
        System.out.println("TwitterDatasourceTest.testErrorInput");
        String message = "This is an error message";
        Data<String> data = new Data<>(Discriminators.Uri.ERROR, message);
        String json = twitter.execute(data.asJson());
        assertNotNull("No JSON returned from the service", json);

        data = Serializer.parse(json, Data.class);
        assertEquals("Invalid discriminator returned", Discriminators.Uri.ERROR, data.getDiscriminator());
        assertEquals("The error message has changed.", message, data.getPayload());
    }

    @Test
    public void testInvalidDiscriminator()
    {
        Data<String> data = new Data<>(Discriminators.Uri.QUERY, "Donald Trump");
        String json = twitter.execute(data.asJson());
        assertNotNull("No JSON returned from the service", json);
        data = Serializer.parse(json, Data.class);
        assertEquals("Invalid discriminator returned: " + data.getDiscriminator(), Discriminators.Uri.ERROR, data.getDiscriminator());
        System.out.println(data.getPayload());
    }

    private void expect(String expected, String actual)
    {
        String message = String.format("Expected: %s Actual: %s", expected, actual);
        assertTrue(message, actual.equals(expected));
    }
}

