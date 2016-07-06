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
import ciir.umass.edu.utilities.RankLibError;
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
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

        IOSpecification requires = new IOSpecification();
        requires.addFormat(Discriminators.Uri.GET);

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

        Data<String> data = Serializer.parse(input, Data.class);
        String discriminator = data.getDiscriminator();
        if (Discriminators.Uri.ERROR.equals(discriminator))
        {
            return input;
        }

        if (!Discriminators.Uri.GET.equals(discriminator))
        {
            return generateError("Invalid discriminator.\nExpected " + Discriminators.Uri.GET + "\nFound " + discriminator);
        }

        // Create a stream to hold the output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        // Save the old System.out!
        PrintStream old = System.out;
        // Tell Java to use special stream
        System.setOut(ps);

        if(data.getParameter("cp") == null) {

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

            if (data.getParameters().size() < 2) {
                StringBuilder usage = new StringBuilder();
                usage.append("Usage: java -jar RankLib.jar <Params>");
                usage.append("\nParams:");
                usage.append("\n  [+] Training (+ tuning and evaluation)");
                usage.append("\n\t-train <file>\t\tTraining data");
                usage.append("\n\t-ranker <type>\t\tSpecify which ranking algorithm to use");
                usage.append("\n\t\t\t\t0: MART (gradient boosted regression tree)");
                usage.append("\n\t\t\t\t1: RankNet");
                usage.append("\n\t\t\t\t2: RankBoost");
                usage.append("\n\t\t\t\t3: AdaRank");
                usage.append("\n\t\t\t\t4: Coordinate Ascent");
                usage.append("\n\t\t\t\t6: LambdaMART");
                usage.append("\n\t\t\t\t7: ListNet");
                usage.append("\n\t\t\t\t8: Random Forests");
                usage.append("\n\t\t\t\t9: Linear regression (L2 regularization)");
                usage.append("\n\t[ -feature <file> ]\tFeature description file: list features to be considered by the learner, each on a separate line");
                usage.append("\n\t\t\t\tIf not specified, all features will be used.");
                //usage.append("\n\t[ -metric2t <metric> ]\tMetric to optimize on the training data. Supported: MAP, NDCG@k, DCG@k, P@k, RR@k, BEST@k, ERR@k (default=" + trainMetric + ")");
                usage.append("\n\t[ -metric2t <metric> ]\tMetric to optimize on the training data. Supported: MAP, NDCG@k, DCG@k, P@k, RR@k, ERR@k (default=" + trainMetric + ")");
                usage.append("\n\t[ -gmax <label> ]\tHighest judged relevance label. It affects the calculation of ERR (default=" + (int) SimpleMath.logBase2(ERRScorer.MAX) + ", i.e. 5-point scale {0,1,2,3,4})");
                usage.append("\n\t[ -qrel <file> ]\tTREC-style relevance judgment file. It only affects MAP and NDCG (default=unspecified)");
                usage.append("\n\t[ -silent ]\t\tDo not print progress messages (which are printed by default)");

                usage.append("\n");
                //usage.append("\n        Use the entire specified training data");
                usage.append("\n\t[ -validate <file> ]\tSpecify if you want to tune your system on the validation data (default=unspecified)");
                usage.append("\n\t\t\t\tIf specified, the final model will be the one that performs best on the validation data");
                usage.append("\n\t[ -tvs <x \\in [0..1]> ]\tIf you don't have separate validation data, use this to set train-validation split to be (x)(1.0-x)");

                usage.append("\n\t[ -save <model> ]\tSave the model learned (default=not-save)");

                usage.append("\n");
                usage.append("\n\t[ -test <file> ]\tSpecify if you want to evaluate the trained model on this data (default=unspecified)");
                usage.append("\n\t[ -tts <x \\in [0..1]> ]\tSet train-test split to be (x)(1.0-x). -tts will override -tvs");
                usage.append("\n\t[ -metric2T <metric> ]\tMetric to evaluate on the test data (default to the same as specified for -metric2t)");

                usage.append("\n");
                usage.append("\n\t[ -norm <method>]\tNormalize all feature vectors (default=no-normalization). Method can be:");
                usage.append("\n\t\t\t\tsum: normalize each feature by the sum of all its values");
                usage.append("\n\t\t\t\tzscore: normalize each feature by its mean/standard deviation");
                usage.append("\n\t\t\t\tlinear: normalize each feature by its min/max values");

                //usage.append("\n");
                //usage.append("\n\t[ -sparse ]\t\tUse sparse representation for all feature vectors (default=dense)");

                usage.append("\n");
                usage.append("\n\t[ -kcv <k> ]\t\tSpecify if you want to perform k-fold cross validation using the specified training data (default=NoCV)");
                usage.append("\n\t\t\t\t-tvs can be used to further reserve a portion of the training data in each fold for validation");
                //usage.append("\n\t\t\t\tData for each fold is created from sequential partitions of the training data.");
                //usage.append("\n\t\t\t\tRandomized partitioning can be done by shuffling the training data in advance.");
                //usage.append("\n\t\t\t\tType \"java -cp bin/RankLib.jar ciir.umass.edu.feature.FeatureManager\" for help with shuffling.");

                usage.append("\n\t[ -kcvmd <dir> ]\tDirectory for models trained via cross-validation (default=not-save)");
                usage.append("\n\t[ -kcvmn <model> ]\tName for model learned in each fold. It will be prefix-ed with the fold-number (default=empty)");

                usage.append("\n");
                usage.append("\n    [-] RankNet-specific parameters");
                usage.append("\n\t[ -epoch <T> ]\t\tThe number of epochs to train (default=" + RankNet.nIteration + ")");
                usage.append("\n\t[ -layer <layer> ]\tThe number of hidden layers (default=" + RankNet.nHiddenLayer + ")");
                usage.append("\n\t[ -node <node> ]\tThe number of hidden nodes per layer (default=" + RankNet.nHiddenNodePerLayer + ")");
                usage.append("\n\t[ -lr <rate> ]\t\tLearning rate (default=" + (new DecimalFormat("###.########")).format(RankNet.learningRate) + ")");

                usage.append("\n");
                usage.append("\n    [-] RankBoost-specific parameters");
                usage.append("\n\t[ -round <T> ]\t\tThe number of rounds to train (default=" + RankBoost.nIteration + ")");
                usage.append("\n\t[ -tc <k> ]\t\tNumber of threshold candidates to search. -1 to use all feature values (default=" + RankBoost.nThreshold + ")");

                usage.append("\n");
                usage.append("\n    [-] AdaRank-specific parameters");
                usage.append("\n\t[ -round <T> ]\t\tThe number of rounds to train (default=" + AdaRank.nIteration + ")");
                usage.append("\n\t[ -noeq ]\t\tTrain without enqueuing too-strong features (default=unspecified)");
                usage.append("\n\t[ -tolerance <t> ]\tTolerance between two consecutive rounds of learning (default=" + AdaRank.tolerance + ")");
                usage.append("\n\t[ -max <times> ]\tThe maximum number of times can a feature be consecutively selected without changing performance (default=" + AdaRank.maxSelCount + ")");

                usage.append("\n");
                usage.append("\n    [-] Coordinate Ascent-specific parameters");
                usage.append("\n\t[ -r <k> ]\t\tThe number of random restarts (default=" + CoorAscent.nRestart + ")");
                usage.append("\n\t[ -i <iteration> ]\tThe number of iterations to search in each dimension (default=" + CoorAscent.nMaxIteration + ")");
                usage.append("\n\t[ -tolerance <t> ]\tPerformance tolerance between two solutions (default=" + CoorAscent.tolerance + ")");
                usage.append("\n\t[ -reg <slack> ]\tRegularization parameter (default=no-regularization)");

                usage.append("\n");
                usage.append("\n    [-] {MART, LambdaMART}-specific parameters");
                usage.append("\n\t[ -tree <t> ]\t\tNumber of trees (default=" + LambdaMART.nTrees + ")");
                usage.append("\n\t[ -leaf <l> ]\t\tNumber of leaves for each tree (default=" + LambdaMART.nTreeLeaves + ")");
                usage.append("\n\t[ -shrinkage <factor> ]\tShrinkage, or learning rate (default=" + LambdaMART.learningRate + ")");
                usage.append("\n\t[ -tc <k> ]\t\tNumber of threshold candidates for tree spliting. -1 to use all feature values (default=" + LambdaMART.nThreshold + ")");
                usage.append("\n\t[ -mls <n> ]\t\tMin leaf support -- minimum % of docs each leaf has to contain (default=" + LambdaMART.minLeafSupport + ")");
                usage.append("\n\t[ -estop <e> ]\t\tStop early when no improvement is observed on validaton data in e consecutive rounds (default=" + LambdaMART.nRoundToStopEarly + ")");

                usage.append("\n");
                usage.append("\n    [-] ListNet-specific parameters");
                usage.append("\n\t[ -epoch <T> ]\t\tThe number of epochs to train (default=" + ListNet.nIteration + ")");
                usage.append("\n\t[ -lr <rate> ]\t\tLearning rate (default=" + (new DecimalFormat("###.########")).format(ListNet.learningRate) + ")");

                usage.append("\n");
                usage.append("\n    [-] Random Forests-specific parameters");
                usage.append("\n\t[ -bag <r> ]\t\tNumber of bags (default=" + RFRanker.nBag + ")");
                usage.append("\n\t[ -srate <r> ]\t\tSub-sampling rate (default=" + RFRanker.subSamplingRate + ")");
                usage.append("\n\t[ -frate <r> ]\t\tFeature sampling rate (default=" + RFRanker.featureSamplingRate + ")");
                int type = (RFRanker.rType.ordinal() - RANKER_TYPE.MART.ordinal());
                usage.append("\n\t[ -rtype <type> ]\tRanker to bag (default=" + type + ", i.e. " + rType[type] + ")");
                usage.append("\n\t[ -tree <t> ]\t\tNumber of trees in each bag (default=" + RFRanker.nTrees + ")");
                usage.append("\n\t[ -leaf <l> ]\t\tNumber of leaves for each tree (default=" + RFRanker.nTreeLeaves + ")");
                usage.append("\n\t[ -shrinkage <factor> ]\tShrinkage, or learning rate (default=" + RFRanker.learningRate + ")");
                usage.append("\n\t[ -tc <k> ]\t\tNumber of threshold candidates for tree spliting. -1 to use all feature values (default=" + RFRanker.nThreshold + ")");
                usage.append("\n\t[ -mls <n> ]\t\tMin leaf support -- minimum % of docs each leaf has to contain (default=" + RFRanker.minLeafSupport + ")");

                usage.append("\n");
                usage.append("\n    [-] Linear Regression-specific parameters");
                usage.append("\n\t[ -L2 <reg> ]\t\tL2 regularization parameter (default=" + LinearRegRank.lambda + ")");

                usage.append("\n");
                usage.append("\n  [+] Testing previously saved models");
                usage.append("\n\t-load <model>\t\tThe model to load");
                usage.append("\n\t\t\t\tMultiple -load can be used to specify models from multiple folds (in increasing order),");
                usage.append("\n\t\t\t\t  in which case the test/rank data will be partitioned accordingly.");
                usage.append("\n\t-test <file>\t\tTest data to evaluate the model(s) (specify either this or -rank but not both)");
                usage.append("\n\t-rank <file>\t\tRank the samples in the specified file (specify either this or -test but not both)");
                usage.append("\n\t[ -metric2T <metric> ]\tMetric to evaluate on the test data (default=" + trainMetric + ")");
                usage.append("\n\t[ -gmax <label> ]\tHighest judged relevance label. It affects the calculation of ERR (default=" + (int) SimpleMath.logBase2(ERRScorer.MAX) + ", i.e. 5-point scale {0,1,2,3,4})");
                usage.append("\n\t[ -score <file>]\tStore ranker's score for each object being ranked (has to be used with -rank)");
                usage.append("\n\t[ -qrel <file> ]\tTREC-style relevance judgment file. It only affects MAP and NDCG (default=unspecified)");
                usage.append("\n\t[ -idv <file> ]\t\tSave model performance (in test metric) on individual ranked lists (has to be used with -test)");
                usage.append("\n\t[ -norm ]\t\tNormalize feature vectors (similar to -norm for training/tuning)\n");
                //usage.append("\n\t[ -sparse ]\t\tUse sparse representation for all feature vectors (default=dense)");

                return generateError(usage.toString());
            }

            if (data.getParameter("train") != null) {
                trainFile = (String) data.getParameter("train");
            }

            if (data.getParameter("ranker") != null) {
                rankerType = (int) data.getParameter("ranker");
            }

            if (data.getParameter("feature") != null) {
                featureDescriptionFile = (String) data.getParameter("feature");
            }

            if (data.getParameter("metric2t") != null) {
                trainMetric = (String) data.getParameter("metric2T");
            }

            if (data.getParameter("metric2T") != null) {
                testMetric = (String) data.getParameter("metric2T");
            }

            if (data.getParameter("gmax") != null) {
                ERRScorer.MAX = Math.pow(2, (double) data.getParameter("gmax"));
            }

            if (data.getParameter("qrel") != null) {
                qrelFile = (String) data.getParameter("qrel");
            }

            if (data.getParameter("tts") != null) {
                ttSplit = (Float) data.getParameter("tts");
            }

            if (data.getParameter("tvs") != null) {
                tvSplit = (Float) data.getParameter("tvs");
            }

            if (data.getParameter("kcv") != null) {
                foldCV = (int) data.getParameter("kcv");
            }

            if (data.getParameter("validate") != null) {
                validationFile = (String) data.getParameter("validate");
            }

            if (data.getParameter("test") != null) {
                testFile = (String) data.getParameter("test");
                testFiles.add(testFile);
            }

            if (data.getParameter("norm") != null) {
                Evaluator.normalize = true;
                String n = (String) data.getParameter("norm");
                if (n.compareTo("sum") == 0)
                    Evaluator.nml = new SumNormalizor();
                else if (n.compareTo("zscore") == 0)
                    Evaluator.nml = new ZScoreNormalizor();
                else if (n.compareTo("linear") == 0)
                    Evaluator.nml = new LinearNormalizer();
                else {
                    return generateError("Unknown normalizor: " + n);
                }
            }

            if (data.getParameter("sparse") != null) {
                useSparseRepresentation = true;
            }

            if (data.getParameter("save") != null) {
                modelFile = (String) data.getParameter("save");
            }

            if (data.getParameter("kcvmd") != null) {
                kcvModelDir = (String) data.getParameter("kcvmd");
            }

            if (data.getParameter("kcvmn") != null) {
                kcvModelFile = (String) data.getParameter("kcvmn");
            }

            if (data.getParameter("silent") != null) {
                Ranker.verbose = false;
            }

            if (data.getParameter("load") != null) {
                savedModelFile = (String) data.getParameter("load");
                savedModelFiles.add(savedModelFile);
            }

            if (data.getParameter("idv") != null) {
                prpFile = (String) data.getParameter("idv");
            }

            if (data.getParameter("rank") != null) {
                rankFile = (String) data.getParameter("rank");
            }

            if (data.getParameter("score") != null) {
                scoreFile = (String) data.getParameter("score");
            }


            //Ranker-specific parameters
            if (data.getParameter("epoch") != null) {
                RankNet.nIteration = (int) data.getParameter("epoch");
                ListNet.nIteration = (int) data.getParameter("epoch");
            }

            if (data.getParameter("layer") != null) {
                RankNet.nHiddenLayer = (int) data.getParameter("layer");
            }


            if (data.getParameter("node") != null) {
                RankNet.nHiddenNodePerLayer = (int) data.getParameter("node");
            }


            if (data.getParameter("lr") != null) {
                RankNet.learningRate = (double) data.getParameter("lr");
                ListNet.learningRate = Neuron.learningRate;
            }

            //RankBoost
            if (data.getParameter("tc") != null) {
                RankBoost.nThreshold = (int) data.getParameter("tc");
                LambdaMART.nThreshold = (int) data.getParameter("tc");
            }

            if (data.getParameter("noeg") != null) {
                AdaRank.trainWithEnqueue = false;
            }

            if (data.getParameter("max") != null) {
                AdaRank.maxSelCount = (int) data.getParameter("max");
            }

            //COORDINATE ASCENT
            if (data.getParameter("r") != null) {
                CoorAscent.nRestart = (int) data.getParameter("r");
            }

            if (data.getParameter("i") != null) {
                CoorAscent.nMaxIteration = (int) data.getParameter("i");
            }

            //ranker-shared parameters
            if (data.getParameter("round") != null) {
                RankBoost.nIteration = (int) data.getParameter("round");
                AdaRank.nIteration = (int) data.getParameter("round");
            }

            if (data.getParameter("reg") != null) {
                CoorAscent.slack = (double) data.getParameter("reg");
                CoorAscent.regularized = true;
            }

            if (data.getParameter("tolerance") != null) {
                AdaRank.tolerance = (double) data.getParameter("tolerance");
                CoorAscent.tolerance = (double) data.getParameter("tolerance");
            }

            //MART / LambdaMART / Random forest
            if (data.getParameter("tree") != null) {
                LambdaMART.nTrees = (int) data.getParameter("tree");
                RFRanker.nTrees = (int) data.getParameter("tree");
            }

            if (data.getParameter("leaf") != null) {
                LambdaMART.nTreeLeaves = (int) data.getParameter("leaf");
                RFRanker.nTreeLeaves = (int) data.getParameter("leaf");
            }

            if (data.getParameter("shrinkage") != null) {
                LambdaMART.learningRate = (float) data.getParameter("shrinkage");
                RFRanker.learningRate = (float) data.getParameter("shrinkage");
            }

            if (data.getParameter("mls") != null) {
                LambdaMART.minLeafSupport = (int) data.getParameter("mls");
                RFRanker.minLeafSupport = LambdaMART.minLeafSupport;
            }

            if (data.getParameter("estop") != null) {
                LambdaMART.nRoundToStopEarly = (int) data.getParameter("estop");
            }

            if (data.getParameter("gcc") != null) {
                LambdaMART.gcCycle = (int) data.getParameter("gcc");
            }

            if (data.getParameter("bag") != null) {
                RFRanker.nBag = (int) data.getParameter("bag");
            }

            if (data.getParameter("srate") != null) {
                RFRanker.subSamplingRate = (float) data.getParameter("srate");
            }

            if (data.getParameter("frate") != null) {
                RFRanker.featureSamplingRate = (float) data.getParameter("frate");
            }

            if (data.getParameter("rtype") != null) {
                int rt = (int) data.getParameter("rtype");
                if (rt == 0 || rt == 6)
                    RFRanker.rType = rType2[rt];
                else {
                    return generateError(rType[rt] + " cannot be bagged. Random Forests only supports MART/LambdaMART.");
                }
            }

            if (data.getParameter("L2") != null) {
                LinearRegRank.lambda = (double) data.getParameter("L2");

            }

            if (data.getParameter("thread") != null) {
                nThread = (int) data.getParameter("thread");
            }

            if (nThread == -1) {
                nThread = Runtime.getRuntime().availableProcessors();
                if (nThread > 4)
                    nThread = 4;
            }
            MyThreadPool.init(nThread);

            if (testMetric.compareTo("") == 0)
                testMetric = trainMetric;

            System.out.println("");
            //System.out.println((keepOrigFeatures)?"Keep orig. features":"Discard orig. features");
            System.out.println("[+] General Parameters:");
            Evaluator e = new Evaluator(rType2[rankerType], trainMetric, testMetric);
            if (trainFile.compareTo("") != 0) {
                System.out.println("Training data:\t" + trainFile);

                //print out parameter settings
                if (foldCV != -1) {
                    System.out.println("Cross validation: " + foldCV + " folds.");
                    if (tvSplit > 0)
                        System.out.println("Train-Validation split: " + tvSplit);
                } else {
                    if (testFile.compareTo("") != 0)
                        System.out.println("Test data:\t" + testFile);
                    else if (ttSplit > 0)//choose to split training data into train and test
                        System.out.println("Train-Test split: " + ttSplit);

                    if (validationFile.compareTo("") != 0)//the user has specified the validation set
                        System.out.println("Validation data:\t" + validationFile);
                    else if (ttSplit <= 0 && tvSplit > 0)
                        System.out.println("Train-Validation split: " + tvSplit);
                }
                System.out.println("Feature vector representation: " + ((useSparseRepresentation) ? "Sparse" : "Dense") + ".");
                System.out.println("Ranking method:\t" + rType[rankerType]);
                if (featureDescriptionFile.compareTo("") != 0)
                    System.out.println("Feature description file:\t" + featureDescriptionFile);
                else
                    System.out.println("Feature description file:\tUnspecified. All features will be used.");
                System.out.println("Train metric:\t" + trainMetric);
                System.out.println("Test metric:\t" + testMetric);

                if (trainMetric.toUpperCase().startsWith("ERR") || testMetric.toUpperCase().startsWith("ERR"))
                    System.out.println("Highest relevance label (to compute ERR): " + (int) SimpleMath.logBase2(ERRScorer.MAX));
                if (qrelFile.compareTo("") != 0)
                    System.out.println("TREC-format relevance judgment (only affects MAP and NDCG scores): " + qrelFile);
                System.out.println("Feature normalization: " + ((Evaluator.normalize) ? Evaluator.nml.name() : "No"));

                if (kcvModelDir.compareTo("") != 0)
                    System.out.println("Models directory: " + kcvModelDir);

                if (kcvModelFile.compareTo("") != 0)
                    System.out.println("Models' name: " + kcvModelFile);

                if (modelFile.compareTo("") != 0)
                    System.out.println("Model file: " + modelFile);
                //System.out.println("#threads:\t" + nThread);

                System.out.println("");
                System.out.println("[+] " + rType[rankerType] + "'s Parameters:");
                RankerFactory rf = new RankerFactory();

                rf.createRanker(rType2[rankerType]).printParameters();
                System.out.println("");

                //starting to do some work
                if (foldCV != -1) {
                    //if(kcvModelDir.compareTo("") != 0 && kcvModelFile.compareTo("") == 0)
                    //	kcvModelFile = "default";
                    //
                    //- Behavioral changes: Write kcv models if kcvmd OR kcvmn defined.  Use
                    //  default names for missing arguments: "kcvmodels" default directory
                    //  and "kcv" default model name.
                    if (kcvModelDir.compareTo("") != 0 && kcvModelFile.compareTo("") == 0) {
                        kcvModelFile = "kcv";
                    } else if (kcvModelDir.compareTo("") == 0 && kcvModelFile.compareTo("") != 0) {
                        kcvModelDir = "kcvmodels";
                    }

                    //- models won't be saved if kcvModelDir=""   [OBSOLETE]
                    //- Models saved if EITHER kcvmd OR kcvmn defined.  Use default names for missing values.
                    e.evaluate(trainFile, featureDescriptionFile, foldCV, tvSplit, kcvModelDir, kcvModelFile);
                } else {
                    if (ttSplit > 0.0)//we should use a held-out portion of the training data for testing?
                        e.evaluate(trainFile, validationFile, featureDescriptionFile, ttSplit);//no validation will be done if validationFile=""
                    else if (tvSplit > 0.0)//should we use a portion of the training data for validation?
                        e.evaluate(trainFile, tvSplit, testFile, featureDescriptionFile);
                    else
                        e.evaluate(trainFile, validationFile, testFile, featureDescriptionFile);//All files except for trainFile can be empty. This will be handled appropriately
                }
            } else //scenario: test a saved model
            {
                System.out.println("Model file:\t" + savedModelFile);
                System.out.println("Feature normalization: " + ((Evaluator.normalize) ? Evaluator.nml.name() : "No"));
                if (rankFile.compareTo("") != 0) {
                    if (scoreFile.compareTo("") != 0) {
                        if (savedModelFiles.size() > 1)//models trained via cross-validation
                            e.score(savedModelFiles, rankFile, scoreFile);
                        else //a single model
                            e.score(savedModelFile, rankFile, scoreFile);
                    } else if (indriRankingFile.compareTo("") != 0) {
                        if (savedModelFiles.size() > 1)//models trained via cross-validation
                            e.rank(savedModelFiles, rankFile, indriRankingFile);
                        else if (savedModelFiles.size() == 1)
                            e.rank(savedModelFile, rankFile, indriRankingFile);
                            //This is *ONLY* for my personal use. It is *NOT* exposed via cmd-line
                            //It will evaluate the input ranking (without being re-ranked by any model) using any measure specified via metric2T
                        else
                            e.rank(rankFile, indriRankingFile);
                    } else {
                        throw RankLibError.create("This function has been removed.\n" +
                                "Consider using -score in addition to your current parameters, " +
                                "and do the ranking yourself based on these scores.");
                        //e.rank(savedModelFile, rankFile);
                    }
                } else {
                    System.out.println("Test metric:\t" + testMetric);
                    if (testMetric.startsWith("ERR"))
                        System.out.println("Highest relevance label (to compute ERR): " + (int) SimpleMath.logBase2(ERRScorer.MAX));

                    if (savedModelFile.compareTo("") != 0) {
                        if (savedModelFiles.size() > 1)//models trained via cross-validation
                        {
                            if (testFiles.size() > 1)
                                e.test(savedModelFiles, testFiles, prpFile);
                            else
                                e.test(savedModelFiles, testFile, prpFile);
                        } else if (savedModelFiles.size() == 1) // a single model
                            e.test(savedModelFile, testFile, prpFile);
                    } else if (scoreFile.compareTo("") != 0)
                        e.testWithScoreFile(testFile, scoreFile);
                        //It will evaluate the input ranking (without being re-ranked by any model) using any measure specified via metric2T
                    else
                        e.test(testFile, prpFile);
                }
            }
            MyThreadPool.getInstance().shutdown();
        }

        // Set System.out back
        System.out.flush();
        System.setOut(old);

        // Output results
        Container container = new Container();
        container.setText(baos.toString());
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
