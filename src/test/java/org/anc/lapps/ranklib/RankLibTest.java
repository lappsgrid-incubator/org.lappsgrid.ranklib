package org.anc.lapps.ranklib;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.ServiceMetadata;
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
        Data<ServiceMetadata> data = Serializer.parse(json, Data.class);
        ServiceMetadata metadata = data.getPayload();
        expect("http://www.lappsgrid.org", metadata.getVendor());
        expect(Discriminators.Uri.ANY, metadata.getAllow());
        expect(Discriminators.Uri.APACHE2, metadata.getLicense());
        expect(Version.getVersion(), metadata.getVersion());
    }

    @Test
    public void testExecuteExample1()
    {
        System.out.println("RankLib.testExecuteExample1");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("params", "-train MQ2008/Fold1/train.txt -test MQ2008/Fold1/test.txt -validate MQ2008/Fold1/vali.txt -ranker 6 -metric2t NDCG@10 -metric2T ERR@10 -save mymodel.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample2()
    {
        System.out.println("RankLib.testExecuteExample2");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("params", "-train MQ2008/Fold1/train.txt -ranker 4 -kcv 5 -kcvmd models/ -kcvmn ca -metric2t NDCG@10 -metric2T ERR@10");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample3()
    {
        System.out.println("RankLib.testExecuteExample3");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("cp", "ciir.umass.edu.Features.FeatureManager");
        data.setParameter("params", "-input MQ2008/Fold1/train.txt -output mydata/ -shuffle");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample4()
    {
        System.out.println("RankLib.testExecuteExample4");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("cp", "ciir.umass.edu.features.FeatureManager");
        data.setParameter("params", "-input MQ2008/Fold1/train.txt -output mydata/ -k 5");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample5()
    {
        System.out.println("RankLib.testExecuteExample5");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("params", "-load mymodel.txt -test MQ2008/Fold1/test.txt -metric2T ERR@10");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample6()
    {
        System.out.println("RankLib.testExecuteExample6");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("params", "-test MQ2008/Fold1/test.txt -metric2T NDCG@10 -idv output/baseline.ndcg.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample7()
    {
        System.out.println("RankLib.testExecuteExample7");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("params", "-load models/f1.ca -test MQ2008/Fold1/test.txt -metric2T NDCG@10 -idv output/f1.ca.ndcg.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample8()
    {
        System.out.println("RankLib.testExecuteExample8");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("params", "-load models/f2.ca -test MQ2008/Fold1/test.txt -metric2T NDCG@10 -idv output/f2.ca.ndcg.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteAnalysis()
    {
        System.out.println("RankLib.testExecuteAnalysis");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("cp", "ciir.umass.edu.eval.Analyzer");
        data.setParameter("params", "-all output/ -base baseline.ndcg.txt > analysis.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample9()
    {
        System.out.println("RankLib.testExecuteExample9");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("params", "-load mymodel.txt -rank MQ2008/Fold1/test.txt -score myscorefile.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testErrorInput()
    {
        System.out.println("RankLib.testErrorInput");
        String message = "This is an error message";
        Data<String> data = new Data<>(Discriminators.Uri.ERROR, message);
        String json = rankLib.execute(data.asJson());
        assertNotNull("No JSON returned from the service", json);

        data = Serializer.parse(json, Data.class);
        assertEquals("Invalid discriminator returned", Discriminators.Uri.ERROR, data.getDiscriminator());
        assertEquals("The error message has changed.", message, data.getPayload());
    }

    @Test
    public void testInvalidDiscriminator()
    {
        System.out.println("RankLib.testInvalidDiscriminator");
        Data<String> data = new Data<>(Discriminators.Uri.QUERY, "");
        String json = rankLib.execute(data.asJson());
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

