package org.anc.lapps.ranklib;

import org.codehaus.groovy.tools.shell.Evaluator;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexandru Mahmoud
 */

public class RankLib implements ProcessingService {

    private String metadata;
    private static final Logger logger = LoggerFactory.getLogger(RankLib.class);

    public RankLib() {

        metadata = generateMetadata();

    }

    private String generateMetadata() {

        ServiceMetadata metadata = new ServiceMetadata();
        metadata.setName(this.getClass().getName());
        metadata.setDescription("RankLib from The Lemur Project");
        metadata.setVersion("1.0.0-SNAPSHOT");
        metadata.setVendor("http://www.lappsgrid.org");
        metadata.setLicense(Discriminators.Uri.APACHE2);

        //Todo: Add input specifications
        IOSpecification requires = new IOSpecification();

        IOSpecification produces = new IOSpecification();
        produces.addFormat(Discriminators.Uri.LAPPS);
        produces.addAnnotation(Discriminators.Uri.TOKEN);
        requires.addLanguage("en");

        metadata.setRequires(requires);
        metadata.setProduces(produces);

        Data<ServiceMetadata> data = new Data<>(Discriminators.Uri.META, metadata);
        return data.asPrettyJson();

    }

    @Override
    /**
     * getMetadata simply returns metadata populated in the constructor
     */
    public String getMetadata() {
        return metadata;
    }


    @Override
    public String execute(String input) {

        String[] rType = new String[]{"MART", "RankNet", "RankBoost", "AdaRank", "Coordinate Ascent", "LambdaRank", "LambdaMART", "ListNet", "Random Forests", "Linear Regression"};
        RANKER_TYPE[] rType2 = new RANKER_TYPE[]{RANKER_TYPE.MART, RANKER_TYPE.RANKNET, RANKER_TYPE.RANKBOOST, RANKER_TYPE.ADARANK, RANKER_TYPE.COOR_ASCENT, RANKER_TYPE.LAMBDARANK, RANKER_TYPE.LAMBDAMART, RANKER_TYPE.LISTNET, RANKER_TYPE.RANDOM_FOREST, RANKER_TYPE.LINEAR_REGRESSION};

        String trainFile = "";
        String featureDescriptionFile = "";
        float ttSplit = 0;//train-test split
        float tvSplit = 0;//train-validation split
        int foldCV = -1;
        String validationFile = "";
        String testFile = "";
        List<String> testFiles = new ArrayList<String>();
        int rankerType = 4;
        String trainMetric = "ERR@10";
        String testMetric = "";
        Evaluator.normalize = false;
        String savedModelFile = "";
        List<String> savedModelFiles = new ArrayList<String>();
        String kcvModelDir = "";
        String kcvModelFile = "";
        String rankFile = "";
        String prpFile = "";

        int nThread = -1; // nThread = #cpu-cores

        StringBuilder out = new StringBuilder();

        Data<String> data = Serializer.parse(input, Data.class);
        String discriminator = data.getDiscriminator();
        if (Discriminators.Uri.ERROR.equals(discriminator))
        {
            return input;
        }
        /*
        if (!Discriminators.Uri.GET.equals(discriminator))
        {
            return generateError("Invalid discriminator.\nExpected " + Discriminators.Uri.GET + "\nFound " + discriminator);
        }
        */

        if(data.getParameter("train") != null) {

        }

        if(data.getParameter("ranker") != null) {

        }

        if(data.getParameter("feature") != null) {

        }

        if(data.getParameter("metric2T") != null) {

        }

        if(data.getParameter("gmax") != null) {

        }

        if(data.getParameter("grel") != null) {

        }

        if(data.getParameter("tts") != null) {

        }

        if(data.getParameter("tvs") != null) {

        }

        if(data.getParameter("kcv") != null) {

        }

        if(data.getParameter("validate") != null) {

        }

        if(data.getParameter("test") != null) {

        }

        if(data.getParameter("norm") != null) {

        }

        if(data.getParameter("sparse") != null) {

        }

        if(data.getParameter("save") != null) {

        }

        if(data.getParameter("kcvmd") != null) {

        }

        if(data.getParameter("kcvmn") != null) {

        }

        if(data.getParameter("silent") != null) {

        }

        if(data.getParameter("load") != null) {

        }

        if(data.getParameter("idv") != null) {

        }

        if(data.getParameter("rank") != null) {

        }

        if(data.getParameter("score") != null) {

        }

        if(data.getParameter("epoch") != null) {

        }


        if(data.getParameter("layer") != null) {

        }


        if(data.getParameter("node") != null) {

        }


        if(data.getParameter("lr") != null) {

        }


        if(data.getParameter("tc") != null) {

        }

        if(data.getParameter("noeg") != null) {

        }

        if(data.getParameter("max") != null) {

        }

        if(data.getParameter("r") != null) {

        }

        if(data.getParameter("i") != null) {

        }

        if(data.getParameter("round") != null) {

        }

        if(data.getParameter("reg") != null) {

        }

        if(data.getParameter("tolerance") != null) {

        }

        if(data.getParameter("tree") != null) {

        }

        if(data.getParameter("leaf") != null) {

        }

        if(data.getParameter("shrinkage") != null) {

        }

        if(data.getParameter("mls") != null) {

        }

        if(data.getParameter("estop") != null) {

        }

        if(data.getParameter("gcc") != null) {

        }

        if(data.getParameter("bag") != null) {

        }

        if(data.getParameter("srate") != null) {

        }

        if(data.getParameter("frate") != null) {

        }

        if(data.getParameter("rtype") != null) {

        }

        if(data.getParameter("L2") != null) {

        }

        if(data.getParameter("thread") != null) {

        }

        if(nThread == -1)
            nThread = Runtime.getRuntime().availableProcessors();
        MyThreadPool.init(nThread);

        if(testMetric.compareTo("")==0)
            testMetric = trainMetric;

        out.append("\n" + "[+] General Parameters:");

        Evaluator e = new Evaluator(rType2[rankerType], trainMetric, testMetric);
        if(trainFile.compareTo("")!=0)
        {
            out.append("\n" + "Training data:\t" + trainFile);

            //print out parameter settings
            if(foldCV != -1)
            {
                out.append("\n" + "Cross validation: " + foldCV + " folds.");
                if(tvSplit > 0)
                    out.append("\n" + "Train-Validation split: " + tvSplit);
            }
            else
            {
                if(testFile.compareTo("") != 0)
                    out.append("\n" + "Test data:\t" + testFile);
                else if(ttSplit > 0)//choose to split training data into train and test
                    out.append("\n" + "Train-Test split: " + ttSplit);

                if(validationFile.compareTo("")!=0)//the user has specified the validation set
                    out.append("\n" + "Validation data:\t" + validationFile);
                else if(ttSplit <= 0 && tvSplit > 0)
                    out.append("\n" + "Train-Validation split: " + tvSplit);
            }
            out.append("\n" + "Feature vector representation: " + ((useSparseRepresentation)?"Sparse":"Dense") + ".");
            out.append("\n" + "Ranking method:\t" + rType[rankerType]);
            if(featureDescriptionFile.compareTo("")!=0)
                out.append("\n" + "Feature description file:\t" + featureDescriptionFile);
            else
                out.append("\n" + "Feature description file:\tUnspecified. All features will be used.");
            out.append("\n" + "Train metric:\t" + trainMetric);
            out.append("\n" + "Test metric:\t" + testMetric);

            if(trainMetric.toUpperCase().startsWith("ERR") || testMetric.toUpperCase().startsWith("ERR"))
                out.append("\n" + "Highest relevance label (to compute ERR): " + (int)SimpleMath.logBase2(ERRScorer.MAX));
            if(qrelFile.compareTo("") != 0)
                out.append("\n" + "TREC-format relevance judgment (only affects MAP and NDCG scores): " + qrelFile);
            out.append("\n" + "Feature normalization: " + ((Evaluator.normalize)?Evaluator.nml.name():"No"));

            if(kcvModelDir.compareTo("")!=0)
                out.append("\n" + "Models directory: " + kcvModelDir);

            if(kcvModelFile.compareTo("")!=0)
                out.append("\n" + "Models' name: " + kcvModelFile);

            if(modelFile.compareTo("")!=0)
                out.append("\n" + "Model file: " + modelFile);
            //out.append("\n" + "#threads:\t" + nThread);

            out.append("\n" + "");
            out.append("\n" + "[+] " + rType[rankerType] + "'s Parameters:");
            RankerFactory rf = new RankerFactory();

            rf.createRanker(rType2[rankerType]).printParameters();
            out.append("\n" + "");

            if(foldCV != -1)
            {
                //  Write kcv models if kcvmd OR kcvmn defined.  Use
                //  default names for missing arguments: "kcvmodels" default directory
                //  and "kcv" default model name.
                if (kcvModelDir.compareTo("") != 0 && kcvModelFile.compareTo("") == 0) {
                    kcvModelFile = "kcv";
                }
                else if(kcvModelDir.compareTo("") == 0 && kcvModelFile.compareTo("") != 0) {
                    kcvModelDir = "kcvmodels";
                }

                //- models won't be saved if kcvModelDir=""   [OBSOLETE]
                //- Models saved if EITHER kcvmd OR kcvmn defined.  Use default names for missing values.
                e.evaluate(trainFile, featureDescriptionFile, foldCV, tvSplit, kcvModelDir, kcvModelFile);
            }
            else
            {
                if(ttSplit > 0.0)//we should use a held-out portion of the training data for testing?
                    e.evaluate(trainFile, validationFile, featureDescriptionFile, ttSplit);//no validation will be done if validationFile=""
                else if(tvSplit > 0.0)//should we use a portion of the training data for validation?
                    e.evaluate(trainFile, tvSplit, testFile, featureDescriptionFile);
                else
                    e.evaluate(trainFile, validationFile, testFile, featureDescriptionFile);//All files except for trainFile can be empty. This will be handled appropriately
            }
        }
        else //scenario: test a saved model
        {
            out.append("\n" + "Model file:\t" + savedModelFile);
            out.append("\n" + "Feature normalization: " + ((Evaluator.normalize)?Evaluator.nml.name():"No"));
            if(rankFile.compareTo("") != 0)
            {
                if(scoreFile.compareTo("") != 0)
                {
                    if(savedModelFiles.size() > 1)//models trained via cross-validation
                        e.score(savedModelFiles, rankFile, scoreFile);
                    else //a single model
                        e.score(savedModelFile, rankFile, scoreFile);
                }
                else if(indriRankingFile.compareTo("") != 0)
                {
                    if(savedModelFiles.size() > 1)//models trained via cross-validation
                        e.rank(savedModelFiles, rankFile, indriRankingFile);
                    else if(savedModelFiles.size() == 1)
                        e.rank(savedModelFile, rankFile, indriRankingFile);
                        //This is *ONLY* for my personal use. It is *NOT* exposed via cmd-line
                        //It will evaluate the input ranking (without being re-ranked by any model) using any measure specified via metric2T
                    else
                        e.rank(rankFile, indriRankingFile);
                }
                else
                {
                    throw RankLibError.create("This function has been removed.\n" +
                            "Consider using -score in addition to your current parameters, " +
                            "and do the ranking yourself based on these scores.");
                }
            }
            else
            {
                out.append("\n" + "Test metric:\t" + testMetric);
                if(testMetric.startsWith("ERR"))
                    out.append("\n" + "Highest relevance label (to compute ERR): " + (int)SimpleMath.logBase2(ERRScorer.MAX));

                if(savedModelFile.compareTo("") != 0)
                {
                    if(savedModelFiles.size() > 1)//models trained via cross-validation
                    {
                        if(testFiles.size() > 1)
                            e.test(savedModelFiles, testFiles, prpFile);
                        else
                            e.test(savedModelFiles, testFile, prpFile);
                    }
                    else if(savedModelFiles.size() == 1) // a single model
                        e.test(savedModelFile, testFile, prpFile);
                }
                else if(scoreFile.compareTo("") != 0)
                    e.testWithScoreFile(testFile, scoreFile);
                    //It will evaluate the input ranking (without being re-ranked by any model) using any measure specified via metric2T
                else
                    e.test(testFile, prpFile);
            }
        }
        MyThreadPool.getInstance().shutdown();


        // Output results
        Container container = new Container();
        container.setText(out.toString());
        Data<Container> output = new Data<>(Discriminators.Uri.LAPPS, container);
        return output.asPrettyJson();
    }

    //main settings
    public static boolean mustHaveRelDoc = false;
    public static boolean useSparseRepresentation = false;
    public static boolean normalize = false;
    public static Normalizer nml = new SumNormalizor();
    public static String modelFile = "";

    public static String qrelFile = "";//measure such as NDCG and MAP requires "complete" judgment.
    //The relevance labels attached to our samples might be only a subset of the entire relevance judgment set.
    //If we're working on datasets like Letor/Web10K or Yahoo! LTR, we can totally ignore this parameter.
    //However, if we sample top-K documents from baseline run (e.g. query-likelihood) to create training data for TREC collections,
    //there's a high chance some relevant document (the in qrel file TREC provides) does not appear in our top-K list -- thus the calculation of
    //MAP and NDCG is no longer precise. If so, specify that "external" relevance judgment here (via the -qrel cmd parameter)

    protected RankerFactory rFact = new RankerFactory();
    protected MetricScorerFactory mFact = new MetricScorerFactory();

    protected MetricScorer trainScorer = null;
    protected MetricScorer testScorer = null;
    protected RANKER_TYPE type = RANKER_TYPE.MART;

    private String generateError(String message)
    {
        Data<String> data = new Data<>();
        data.setDiscriminator(Discriminators.Uri.ERROR);
        data.setPayload(message);
        return data.asPrettyJson();
    }
}
