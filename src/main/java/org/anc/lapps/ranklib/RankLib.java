package org.anc.lapps.ranklib;

import ciir.umass.edu.eval.Evaluator;
import ciir.umass.edu.features.LinearNormalizer;
import ciir.umass.edu.features.Normalizer;
import ciir.umass.edu.features.SumNormalizor;
import ciir.umass.edu.features.ZScoreNormalizor;
import ciir.umass.edu.learning.*;
import ciir.umass.edu.learning.boosting.AdaRank;
import ciir.umass.edu.learning.boosting.RankBoost;
import ciir.umass.edu.learning.neuralnet.ListNet;
import ciir.umass.edu.learning.neuralnet.Neuron;
import ciir.umass.edu.learning.neuralnet.RankNet;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.learning.tree.RFRanker;
import ciir.umass.edu.metric.ERRScorer;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.MetricScorerFactory;
import ciir.umass.edu.utilities.MyThreadPool;
import ciir.umass.edu.utilities.SimpleMath;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static ciir.umass.edu.eval.Evaluator.modelFile;
import static ciir.umass.edu.eval.Evaluator.qrelFile;
import static ciir.umass.edu.eval.Evaluator.useSparseRepresentation;

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
        metadata.setVersion(Version.getVersion());
        metadata.setVendor("http://www.lappsgrid.org");
        metadata.setLicense(Discriminators.Uri.APACHE2);

        //Todo: Add input specifications
        IOSpecification requires = new IOSpecification();

        IOSpecification produces = new IOSpecification();
        produces.addFormat(Discriminators.Uri.LAPPS);
        produces.addAnnotation(Discriminators.Uri.TOKEN);

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
        String indriRankingFile = "";
        String scoreFile = "";

        int nThread = -1; // nThread = #cpu-cores

        StringBuilder out = new StringBuilder();

        Data<String> data = Serializer.parse(input, Data.class);
        String discriminator = data.getDiscriminator();
        if (Discriminators.Uri.ERROR.equals(discriminator))
        {
            return input;
        }

        // TODO: add check of input format
        /*
        if (!Discriminators.Uri.GET.equals(discriminator))
        {
            return generateError("Invalid discriminator.\nExpected " + Discriminators.Uri.GET + "\nFound " + discriminator);
        }
        */

        if(data.getParameter("train") != null) {
            trainFile = (String) data.getParameter("train");
        }

        if(data.getParameter("ranker") != null) {
            rankerType = (int) data.getParameter("ranker");
        }

        if(data.getParameter("feature") != null) {
            featureDescriptionFile = (String) data.getParameter("feature");
        }

        if(data.getParameter("metric2t") != null) {
            trainMetric = (String) data.getParameter("metric2T");
        }

        if(data.getParameter("metric2T") != null) {
            testMetric = (String) data.getParameter("metric2T");
        }

        if(data.getParameter("gmax") != null) {
            ERRScorer.MAX = Math.pow(2, (double) data.getParameter("gmax"));
        }

        if(data.getParameter("qrel") != null) {
            qrelFile = (String) data.getParameter("qrel");
        }

        if(data.getParameter("tts") != null) {
            ttSplit = (Float) data.getParameter("tts");
        }

        if(data.getParameter("tvs") != null) {
            tvSplit = (Float) data.getParameter("tvs");
        }

        if(data.getParameter("kcv") != null) {
            foldCV = (int) data.getParameter("kcv");
        }

        if(data.getParameter("validate") != null) {
            validationFile = (String) data.getParameter("validate");
        }

        if(data.getParameter("test") != null) {
            testFile = (String) data.getParameter("test");
            testFiles.add(testFile);
        }

        if(data.getParameter("norm") != null) {
            Evaluator.normalize = true;
            String n = (String) data.getParameter("norm");
            if(n.compareTo("sum") == 0)
                Evaluator.nml = new SumNormalizor();
            else if(n.compareTo("zscore") == 0)
                Evaluator.nml = new ZScoreNormalizor();
            else if(n.compareTo("linear") == 0)
                Evaluator.nml = new LinearNormalizer();
            else
            {
                return generateError("Unknown normalizor: " + n);
            }
        }

        if(data.getParameter("sparse") != null) {
            useSparseRepresentation = true;
        }

        if(data.getParameter("save") != null) {
            modelFile = (String) data.getParameter("save");
        }

        if(data.getParameter("kcvmd") != null) {
            kcvModelDir = (String) data.getParameter("kcvmd");
        }

        if(data.getParameter("kcvmn") != null) {
            kcvModelFile = (String) data.getParameter("kcvmn");
        }

        if(data.getParameter("silent") != null) {
            Ranker.verbose = false;
        }

        if(data.getParameter("load") != null) {
            savedModelFile = (String) data.getParameter("load");
            savedModelFiles.add(savedModelFile);
        }

        if(data.getParameter("idv") != null) {
            prpFile = (String) data.getParameter("idv");
        }

        if(data.getParameter("rank") != null) {
            rankFile = (String) data.getParameter("rank");
        }

        if(data.getParameter("score") != null) {
            scoreFile = (String) data.getParameter("score");
        }


        //Ranker-specific parameters
        if(data.getParameter("epoch") != null) {
            RankNet.nIteration = (int) data.getParameter("epoch");
            ListNet.nIteration = (int) data.getParameter("epoch");
        }

        if(data.getParameter("layer") != null) {
            RankNet.nHiddenLayer = (int) data.getParameter("layer");
        }


        if(data.getParameter("node") != null) {
            RankNet.nHiddenNodePerLayer = (int) data.getParameter("node");
        }


        if(data.getParameter("lr") != null) {
            RankNet.learningRate = (double) data.getParameter("lr");
            ListNet.learningRate = Neuron.learningRate;
        }

        //RankBoost
        if(data.getParameter("tc") != null) {
            RankBoost.nThreshold = (int) data.getParameter("tc");
            LambdaMART.nThreshold = (int) data.getParameter("tc");
        }

        if(data.getParameter("noeg") != null) {
            AdaRank.trainWithEnqueue = false;
        }

        if(data.getParameter("max") != null) {
            AdaRank.maxSelCount = (int) data.getParameter("max");
        }

            //COORDINATE ASCENT
        if(data.getParameter("r") != null) {
            CoorAscent.nRestart = (int) data.getParameter("r");
        }

        if(data.getParameter("i") != null) {
            CoorAscent.nMaxIteration = (int) data.getParameter("i");
        }

        //ranker-shared parameters
        if(data.getParameter("round") != null) {
            RankBoost.nIteration = (int) data.getParameter("round");
            AdaRank.nIteration = (int) data.getParameter("round");
        }

        if(data.getParameter("reg") != null) {
            CoorAscent.slack = (double) data.getParameter("reg");
            CoorAscent.regularized = true;
        }

        if(data.getParameter("tolerance") != null) {
            AdaRank.tolerance = (double) data.getParameter("tolerance");
            CoorAscent.tolerance = (double) data.getParameter("tolerance");
        }

        //MART / LambdaMART / Random forest
        if(data.getParameter("tree") != null) {
            LambdaMART.nTrees = (int) data.getParameter("tree");
            RFRanker.nTrees = (int) data.getParameter("tree");
        }

        if(data.getParameter("leaf") != null) {
            LambdaMART.nTreeLeaves = (int) data.getParameter("leaf");
            RFRanker.nTreeLeaves = (int) data.getParameter("leaf");
        }

        if(data.getParameter("shrinkage") != null) {
            LambdaMART.learningRate = (float) data.getParameter("shrinkage");
            RFRanker.learningRate = (float) data.getParameter("shrinkage");
        }

        if(data.getParameter("mls") != null) {
            LambdaMART.minLeafSupport = (int) data.getParameter("mls");
            RFRanker.minLeafSupport = LambdaMART.minLeafSupport;
        }

        if(data.getParameter("estop") != null) {
            LambdaMART.nRoundToStopEarly = (int) data.getParameter("estop");
        }

        if(data.getParameter("gcc") != null) {
            LambdaMART.gcCycle = (int) data.getParameter("gcc");
        }

        if(data.getParameter("bag") != null) {
            RFRanker.nBag = (int) data.getParameter("bag");
        }

        if(data.getParameter("srate") != null) {
            RFRanker.subSamplingRate = (float) data.getParameter("srate");
        }

        if(data.getParameter("frate") != null) {
            RFRanker.featureSamplingRate = (float) data.getParameter("frate");
        }

        if(data.getParameter("rtype") != null) {
            int rt = (int) data.getParameter("rtype");
            if(rt == 0 || rt == 6)
                RFRanker.rType = rType2[rt];
            else
            {
                return generateError(rType[rt] + " cannot be bagged. Random Forests only supports MART/LambdaMART.");
            }
        }

        if(data.getParameter("L2") != null) {
            LinearRegRank.lambda = (double) data.getParameter("L2");

        }

        if(data.getParameter("thread") != null) {
            nThread = (int) data.getParameter("thread");
        }

        if(nThread == -1) {
            nThread = Runtime.getRuntime().availableProcessors();
            if(nThread > 4)
                nThread = 4;
        }
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
            out.append("Model file:\t" + savedModelFile);
            out.append("Feature normalization: " + ((Evaluator.normalize)?Evaluator.nml.name():"No"));
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
                    return generateError("This function has been removed.\n" +
                            "Consider using -score in addition to your current parameters, " +
                            "and do the ranking yourself based on these scores.");
                    //e.rank(savedModelFile, rankFile);
                }
            }
            else
            {
                out.append("Test metric:\t" + testMetric);
                if(testMetric.startsWith("ERR"))
                    out.append("Highest relevance label (to compute ERR): " + (int)SimpleMath.logBase2(ERRScorer.MAX));

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


    private String generateError(String message)
    {
        Data<String> data = new Data<>();
        data.setDiscriminator(Discriminators.Uri.ERROR);
        data.setPayload(message);
        return data.asPrettyJson();
    }
}
