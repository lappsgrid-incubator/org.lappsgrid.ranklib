package org.anc.lapps.ranklib;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import java.util.Map;

import static org.junit.Assert.*;

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
        String json = rankLib.getMetadata();
        assertNotNull("service.getMetadata() returned null", json);

        Data data = Serializer.parse(json, Data.class);
        assertNotNull("Unable to parse metadata json.", data);
        assertNotSame(data.getPayload().toString(), Discriminators.Uri.ERROR, data.getDiscriminator());

        ServiceMetadata metadata = new ServiceMetadata((Map) data.getPayload());

        assertEquals("Vendor is not correct", "http://www.lappsgrid.org", metadata.getVendor());
        assertEquals("Name is not correct", RankLib.class.getName(), metadata.getName());
        assertEquals("Version is not correct.","1.0-SNAPSHOT" , metadata.getVersion());
        assertEquals("License is not correct", Discriminators.Uri.APACHE2, metadata.getLicense());

        IOSpecification produces = metadata.getProduces();
        assertEquals("Produces encoding is not correct", "UTF-8", produces.getEncoding());
        assertEquals("Too many annotation types produced", 1, produces.getAnnotations().size());
        assertEquals("Tokens not produced", Discriminators.Uri.TOKEN, produces.getAnnotations().get(0));
        assertEquals("Too many output formats", 1, produces.getFormat().size());
        assertEquals("LIF not produced", Discriminators.Uri.LAPPS, produces.getFormat().get(0));

        IOSpecification requires = metadata.getRequires();
        assertEquals("Requires encoding is not correct", "UTF-8", requires.getEncoding());
        assertEquals("Requires Discriminator is not correct", Discriminators.Uri.GET, requires.getFormat().get(0));
    }

    @Test
    public void testExecuteExample1()
    {
        System.out.println("RankLib.testExecuteExample1");

        Data<String> data = new Data<>(Discriminators.Uri.GET);

        data.setPayload("-train MQ2008/Fold1/train.txt -test MQ2008/Fold1/test.txt -validate MQ2008/Fold1/vali.txt -ranker 6 -metric2t NDCG@10 -metric2T ERR@10 -save mymodel.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample2()
    {
        System.out.println("RankLib.testExecuteExample2");

        Data<String> data = new Data<>(Discriminators.Uri.GET);

        data.setPayload("-train MQ2008/Fold1/train.txt -ranker 4 -kcv 5 -kcvmd models/ -kcvmn ca -metric2t NDCG@10 -metric2T ERR@10");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample3()
    {
        System.out.println("RankLib.testExecuteExample3");

        Data<String> data = new Data<>(Discriminators.Uri.GET);

        data.setParameter("cp", "ciir.umass.edu.Features.FeatureManager");
        data.setPayload("-input MQ2008/Fold1/train.txt -output mydata/ -shuffle");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample4()
    {
        System.out.println("RankLib.testExecuteExample4");

        Data<String> data = new Data<>(Discriminators.Uri.GET);

        data.setParameter("cp", "ciir.umass.edu.features.FeatureManager");
        data.setPayload("-input MQ2008/Fold1/train.txt -output mydata/ -k 5");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample5()
    {
        System.out.println("RankLib.testExecuteExample5");

        Data<String> data = new Data<>(Discriminators.Uri.GET);

        data.setPayload("-load mymodel.txt -test MQ2008/Fold1/test.txt -metric2T ERR@10");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample6()
    {
        System.out.println("RankLib.testExecuteExample6");

        Data<String> data = new Data<>(Discriminators.Uri.GET);

        data.setPayload("-test MQ2008/Fold1/test.txt -metric2T NDCG@10 -idv output/baseline.ndcg.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample7()
    {
        System.out.println("RankLib.testExecuteExample7");

        Data<String> data = new Data<>(Discriminators.Uri.GET);

        data.setPayload("-load models/f1.ca -test MQ2008/Fold1/test.txt -metric2T NDCG@10 -idv output/f1.ca.ndcg.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample8()
    {
        System.out.println("RankLib.testExecuteExample8");

        Data<String> data = new Data<>(Discriminators.Uri.GET);

        data.setPayload("-load models/f2.ca -test MQ2008/Fold1/test.txt -metric2T NDCG@10 -idv output/f2.ca.ndcg.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteAnalysis()
    {
        System.out.println("RankLib.testExecuteAnalysis");

        Data<String> data = new Data<>(Discriminators.Uri.GET);

        data.setParameter("cp", "ciir.umass.edu.eval.Analyzer");
        data.setPayload("-all output/ -base baseline.ndcg.txt");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteExample9()
    {
        System.out.println("RankLib.testExecuteExample9");

        Data<String> data = new Data<>(Discriminators.Uri.GET);

        data.setPayload("-load mymodel.txt -rank MQ2008/Fold1/test.txt -score myscorefile.txt");

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
        Data<String> data = new Data<>(Discriminators.Uri.QUERY, "-train");
        String json = rankLib.execute(data.asJson());
        assertNotNull("No JSON returned from the service", json);
        data = Serializer.parse(json, Data.class);
        assertEquals("Invalid discriminator returned: " + data.getDiscriminator(), Discriminators.Uri.ERROR, data.getDiscriminator());
        System.out.println(data.getPayload());
    }


}

