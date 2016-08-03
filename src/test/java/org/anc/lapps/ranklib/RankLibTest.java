package org.anc.lapps.ranklib;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
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
        String jsonMetadata = rankLib.getMetadata();
        assertNotNull("service.getMetadata() returned null", jsonMetadata);

        Data data = Serializer.parse(jsonMetadata, Data.class);
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
    public void testExecuteTraining1()
    {
        System.out.println("RankLib.testExecuteTraining1");

        String trainTxt = "";
        String testTxt = "";
        String validateTxt = "";

        try
        {
            trainTxt = rankLib.readFile("MQ2008/Fold1/train.txt");
            testTxt = rankLib.readFile("MQ2008/Fold1/test.txt");
            validateTxt = rankLib.readFile("MQ2008/Fold1/vali.txt");
        }
        catch(IOException e) { }

        Map<String,String> payload = new HashMap<>();
        payload.put("train", trainTxt);
        payload.put("test", testTxt);
        payload.put("validate", validateTxt);
        String jsonPayload = Serializer.toJson(payload);

        Data<String> data = new Data<>(Discriminators.Uri.GET, jsonPayload);

        data.setParameter("ranker", "6");
        data.setParameter("metric2t", "NDCG@10");
        data.setParameter("metric2T", "ERR@10");
        data.setParameter("save", "myModel");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteTraining2()
    {
        System.out.println("RankLib.testExecuteTraining2");

        String trainTxt = "";

        try
        {
            trainTxt = rankLib.readFile("MQ2008/Fold1/train.txt");
        }
        catch(IOException e) { }

        Map<String,String> payload = new HashMap<>();
        payload.put("train", trainTxt);
        String jsonPayload = Serializer.toJson(payload);

        Data<String> data = new Data<>(Discriminators.Uri.GET, jsonPayload);

        data.setParameter("ranker", "4");
        data.setParameter("kcv", "5");
        data.setParameter("saveCrossValidation", "ca");
        data.setParameter("metric2t", "NDCG@10");
        data.setParameter("metric2T", "ERR@10");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteFeatureManager1()
    {
        System.out.println("RankLib.testExecuteFeatureManager1");

        String inputTxt = "";

        try
        {
            inputTxt = rankLib.readFile("MQ2008/Fold1/train.txt");
        }
        catch(IOException e) { }

        Map<String,String> payload = new HashMap<>();
        payload.put("input", inputTxt);
        String jsonPayload = Serializer.toJson(payload);

        Data<String> data = new Data<>(Discriminators.Uri.GET, jsonPayload);

        data.setParameter("function", "Manage");
        data.setParameter("shuffle", true);

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteFeatureManager2()
    {
        System.out.println("RankLib.testExecuteFeatureManager2");

        String inputTxt = "";

        try
        {
            inputTxt = rankLib.readFile("MQ2008/Fold1/train.txt");
        }
        catch(IOException e) { }

        Map<String,String> payload = new HashMap<>();
        payload.put("input", inputTxt);
        String jsonPayload = Serializer.toJson(payload);

        Data<String> data = new Data<>(Discriminators.Uri.GET, jsonPayload);

        data.setParameter("function", "FeatureManager");
        data.setParameter("k", "5");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteEvaluator1()
    {
        System.out.println("RankLib.testExecuteEvaluator1");

        String loadTxt = "";
        String testTxt = "";

        try
        {
            loadTxt = rankLib.readFile("mymodel.txt");
            testTxt = rankLib.readFile("MQ2008/Fold1/test.txt");
        }
        catch(IOException e) { }

        Map<String,String> payload = new HashMap<>();
        payload.put("load", loadTxt);
        payload.put("test", testTxt);
        String jsonPayload = Serializer.toJson(payload);

        Data<String> data = new Data<>(Discriminators.Uri.GET, jsonPayload);

        data.setParameter("metric2T", "ERR@10");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteEvaluator2()
    {
        System.out.println("RankLib.testExecuteEvaluator2");

        String testTxt = "";

        try
        {
            testTxt = rankLib.readFile("MQ2008/Fold1/test.txt");
        }
        catch(IOException e) { }

        Map<String,String> payload = new HashMap<>();
        payload.put("test", testTxt);
        String jsonPayload = Serializer.toJson(payload);

        Data<String> data = new Data<>(Discriminators.Uri.GET, jsonPayload);

        data.setParameter("test", "MQ2008/Fold1/test.txt");
        data.setParameter("metric2T", "NDCG@10");
        data.setParameter("idv", "baseline.ndcg");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteEvaluator3()
    {
        System.out.println("RankLib.testExecuteEvaluator3");

        String loadTxt = "";
        String testTxt = "";

        try
        {
            loadTxt = rankLib.readFile("models/f1.ca");
            testTxt = rankLib.readFile("MQ2008/Fold1/test.txt");
        }
        catch(IOException e) { }

        Map<String,String> payload = new HashMap<>();
        payload.put("load", loadTxt);
        payload.put("test", testTxt);
        String jsonPayload = Serializer.toJson(payload);

        Data<String> data = new Data<>(Discriminators.Uri.GET, jsonPayload);

        data.setParameter("metric2T", "NDCG@10");
        data.setParameter("idv", "f1.ca.ndcg");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteEvaluator4()
    {
        System.out.println("RankLib.testExecuteEvaluator4");

        String loadTxt = "";
        String testTxt = "";

        try
        {
            loadTxt = rankLib.readFile("models/f2.ca");
            testTxt = rankLib.readFile("MQ2008/Fold1/test.txt");
        }
        catch(IOException e) { }

        Map<String,String> payload = new HashMap<>();
        payload.put("load", loadTxt);
        payload.put("test", testTxt);
        String jsonPayload = Serializer.toJson(payload);

        Data<String> data = new Data<>(Discriminators.Uri.GET, jsonPayload);

        data.setParameter("metric2T", "NDCG@10");
        data.setParameter("idv", "f2.ca.ndcg");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteAnalyzer()
    {
        System.out.println("RankLib.testExecuteAnalyzer");

        String baselineTxt = "";
        String file1Txt = "";
        String file2Txt = "";

        try
        {
            baselineTxt = rankLib.readFile("output/baseline.ndcg.txt");
            file1Txt = rankLib.readFile("output/f1.ca.ndcg.txt");
            file2Txt = rankLib.readFile("output/f2.ca.ndcg.txt");

        }
        catch(IOException e) { }

        Map<String,String> payload = new HashMap<>();
        payload.put("baseline", baselineTxt);
        payload.put("file1", file1Txt);
        payload.put("file2", file2Txt);
        String jsonPayload = Serializer.toJson(payload);

        Data<String> data = new Data<>(Discriminators.Uri.GET, jsonPayload);

        data.setParameter("function", "Analyzer");

        String response = rankLib.execute(data.asJson());
        System.out.println(response);
    }

    @Test
    public void testExecuteEvaluator5()
    {
        System.out.println("RankLib.testExecuteEvaluator5");

        String loadTxt = "";
        String rankTxt = "";

        try
        {
            loadTxt = rankLib.readFile("mymodel.txt");
            rankTxt = rankLib.readFile("MQ2008/Fold1/test.txt");
        }
        catch(IOException e) { }

        Map<String,String> payload = new HashMap<>();
        payload.put("load", loadTxt);
        payload.put("rank", rankTxt);
        String jsonPayload = Serializer.toJson(payload);

        Data<String> data = new Data<>(Discriminators.Uri.GET, jsonPayload);

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
        Data<String> data = new Data<>(Discriminators.Uri.QUERY, "-train");
        String json = rankLib.execute(data.asJson());
        assertNotNull("No JSON returned from the service", json);
        data = Serializer.parse(json, Data.class);
        assertEquals("Invalid discriminator returned: " + data.getDiscriminator(), Discriminators.Uri.ERROR, data.getDiscriminator());
        System.out.println(data.getPayload());
    }

}

