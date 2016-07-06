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
        expect(Version.getVersion(), metadata.getVersion());
    }

    @Test
    public void testExecuteExample1()
    {
        System.out.println("RankLib.testExecuteExample1");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("jar", "bin/RankLib.jar");
        data.setParameter("train", "MQ2008/Fold1/train.txt");
        data.setParameter("test", "MQ2008/Fold1/test.txt");
        data.setParameter("validate", "MQ2008/Fold1/vali.txt");
        data.setParameter("ranker", 6);
        data.setParameter("metric2t", "NDCG@10");
        data.setParameter("metric2T", "ERR@10");
        data.setParameter("save", "mymodel.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample2()
    {
        System.out.println("RankLib.testExecuteExample2");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("jar", "bin/RankLib.jar");
        data.setParameter("train", "MQ2008/Fold1/train.txt");
        data.setParameter("ranker", 4);
        data.setParameter("kcv", 5);
        data.setParameter("kcvmd", "models/");
        data.setParameter("kcvmn", "ca");
        data.setParameter("metric2t", "NDCG@10");
        data.setParameter("metric2T", "ERR@10");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample3()
    {
        System.out.println("RankLib.testExecuteExample3");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("cp", "bin/RankLib.jar ciir.umass.edu.Features.FeatureManager");
        data.setParameter("input", "MQ2008/Fold1/train.txt");
        data.setParameter("output", "mydata/");
        data.setParameter("shuffle", true);

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample4()
    {
        System.out.println("RankLib.testExecuteExample4");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("cp", "bin/RankLib.jar ciir.umass.edu.features.FeatureManager");
        data.setParameter("input", "MQ2008/Fold1/train.txt.shuffled");
        data.setParameter("output", "mydata/");
        data.setParameter("k", 5);

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample5()
    {
        System.out.println("RankLib.testExecuteExample5");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("jar", "bin/RankLib.jar");
        data.setParameter("load", "mymodel.txt");
        data.setParameter("test", "MQ2008/Fold1/test.txt");
        data.setParameter("metric2T", "ERR@10");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample6()
    {
        System.out.println("RankLib.testExecuteExample6");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("jar", "bin/RankLib.jar");
        data.setParameter("test", "MQ2008/Fold1/test.txt");
        data.setParameter("metric2T", "NDCG@10");
        data.setParameter("idv", "output/baseline.ndcg.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample7()
    {
        System.out.println("RankLib.testExecuteExample7");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("jar", "bin/RankLib.jar");
        data.setParameter("load", "ca.model.txt");
        data.setParameter("test", "MQ2008/Fold1/test.txt");
        data.setParameter("metric2T", "NDCG@10");
        data.setParameter("idv", "output/ca.ndcg.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample8()
    {
        System.out.println("RankLib.testExecuteExample8");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("jar", "bin/RankLib.jar");
        data.setParameter("load", "lm.model.txt");
        data.setParameter("test", "MQ2008/Fold1/test.txt");
        data.setParameter("metric2T", "NDCG@10");
        data.setParameter("idv", "output/lm.ndcg.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteAnalysis()
    {
        System.out.println("RankLib.testExecuteAnalysis");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("cp", "bin/RankLib.jar ciir.umass.edu.eval.Analyzer");
        data.setParameter("all", "output/");
        data.setParameter("base", "baseline.ndcg.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample9()
    {
        System.out.println("RankLib.testExecuteExample9");

        Data<String> data = new Data<>(Discriminators.Uri.GET, "");

        data.setParameter("jar", "bin/RankLib.jar");
        data.setParameter("load", "mymodel.txt");
        data.setParameter("rank", "MQ2008/Fold1/test.txt");
        data.setParameter("score", "myscorefile.txt");

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

