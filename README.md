# org.lappsgrid.ranklib
RankLib from the [Lemur Project](https://sourceforge.net/p/lemur/wiki/RankLib/).

## Usage

Takes a Data<String> object with a [Uri.GET](http://vocab.lappsgrid.org/ns/action/get) discriminator and a String payload representing the json format of a HashMap<String,String> containing all needed input. Additional program-specific parameters can be added following the list below.

## Function Parameter

The main parameter is the "function" parameter which specifies which of the four functions of RankLib one aims to use.

| Function | Original RankLib Command Equivalent |
| --- | --- |
| Train | equivalent to "java -jar RankLib.jar <Params>" command with -train parameter |
| Evaluate | equivalent to "java -jar RankLib.jar <Params>" command with -load parameter |
| Analyze | equivalent to "java -cp bin/RankLib.jar ciir.umass.edu.eval.Analyzer <Params>" command |
| Manage | equivalent to "java -cp bin/RankLib.jar ciir.umass.edu.features.FeatureManager <Params>" command |

Each function has specific input requirements and parameters:

* [Train](md/train/main.md).
* [Evaluate](md/evaluate/main.md).
* [Analyze](md/analyze/main.md).
* [Manage](md/analyze/main.md).

# Training (+ tuning and evaluation) function

The evaluator function has two main uses. The first consists of training (+ tuning and evaluating), while the second consists of testing previously saved models.

# Training (+ tuning and evalution)

These are the possible input keys that can be given to the Evaluator function.

| Key | Description |
| --- | --- |
| train | Training data |
| feature | (Optional) Feature description file: list features to be considered by the learner, each on a separate line (default=all features will be used) |
| qrel | (Optional) TREC-style relevance judgment file. It only affects MAP and NDCG (default=unspecified) |
| validate | (Optional) Specify if you want to tune your system on the validation data (default=unspecified). If specified, the final model will be the one that performs best on the validation data |
| test | (Optional) Specify if you want to evaluate the trained model on this data (default=unspecified) |

Additionally, the Evaluator function takes the parameters listed in the table below. All parameters take their arguments as strings.

| Parameter Name | Description and arguments |
| --- | --- |
| ranker | Specify which ranking algorithm to use (by number) |
| | 0: MART (gradient boosted regression tree) |
| | 1: RankNet |
| | 2: RankBoost |
| | 3: AdaRank |
| | 4: Coordinate Ascent |
| | 6: LambdaMART |
| | 7: ListNet |
| | 8: Random Forests |
| | 9: Linear regression (L2 regularization) |
| metric2t | (Optional) Metric to optimize on the training data. Supported: MAP, NDCG@k, DCG@k, P@k, RR@k, ERR@k (default=ERR@10) |
| metric2T | (Optional) Metric to evaluate on the test data (default = same as specified for metric2t) |
| gmax | Highest judged relevance label. It affects the calculation of ERR (default=4, i.e. 5-point scale {0,1,2,3,4}) |
| silent | (Optional) Do not print progress messages (which are printed by default). This is a boolean parameter: it will be set if it has any argument. If you wish to not have it set, simply leave it null |
| tvs | (Optional) Takes a value x between 0 and 1. If you don't have separate validation data, use this to set train-validation split to be (x)(1.0-x) |
| tts | (Optional) Takes a value x between 0 and 1. Set train-test split to be (x)(1.0-x). tts will override tvs |
| save | (Optional) Name of the model learned. Setting this parameter will save the model (default=not-save) |
| norm | (Optional) Normalize all feature vectors (default=no-normalization). Method can be:
| | sum: normalize each feature by the sum of all its values |
| | zscore: normalize each feature by its mean/standard deviation |
| | linear: normalize each feature by its min/max values |
| kcv | (Optional) Specify if you want to perform k-fold cross validation using the specified training data (default=NoCV). tvs can be used to further reserve a portion of the training data in each fold for validation |
| saveCrossValidation | (Optional) Name for model learned in each fold, which will be prefix-ed with the fold-number. Setting this parameter will save all the models learned in each fold (default=not-save) |

The following OPTIONAL parameters are ranker-specific.

| RankNet-specific Parameter Name | Description and arguments |
| --- | --- |
| epoch  | The number of epochs to train (default=100) |
| layer | The number of hidden layers (default=1) |
| node | The number of hidden nodes per layer (default=10) |
| lr | Learning rate (default=0.00005) |

| RankBoost-specific Parameter Name | Description and arguments |
| --- | --- |
| round | The number of rounds to train (default=300) |
| tc | The number of threshold candidates to search. -1 to use all feature values (default=10) |

| AdaRank-specific Parameter Name | Description and arguments |
| --- | --- |
| round | The number of rounds to train (default=500) |
| noeq | Train without enqueuing too-strong features (default=unspecified). This is a boolean parameter: it will be set if it has any argument. If you wish to not have it set, simply leave it null |
| tolerance | Tolerance between two consecutive rounds of learning (default=0.002) |
| max | The maximum number of times can a feature be consecutively selected without changing performance (default=5) |

| Coordinate Ascent-specific Parameter Name | Description and arguments |
| --- | --- |
| r | The number of random restarts (default=5) |
| i | The number of iterations to search in each dimension (default=25) |
| tolerance  | Performance tolerance between two solutions (default=0.001) |
| reg | Regularization parameter (default=no-regularization) |


| {MART, LambdaMART}-specific Parameter Name | Description and arguments |
| --- | --- |
| tree  | Number of trees (default=1000) |
| leaf | Number of leaves for each tree (default=10) |
| shrinkage | Shrinkage, or learning rate (default=0.1) |
| tc | Number of threshold candidates for tree spliting. -1 to use all feature values (default=256) |
| mls | Min leaf support -- minimum % of docs each leaf has to contain (default=1) |
| estop | Stop early when no improvement is observed on validaton data in e consecutive rounds (default=100) |

| ListNet-specific Parameter Name | Description and arguments |
| --- | --- |
| epoch | The number of epochs to train (default=1500) |
| lr | Learning rate (default=0.00001) |

| Random Forests-specific Parameter Name | Description and arguments |
| --- | --- |
| bag | The number of bags (default=300) |
| srate | Sub-sampling rate (default=1.0) |
| frate | Feature sampling rate (default=0.3) |
| rtype | Ranker to bag (default=0, i.e. MART) |
| tree | Number of trees in each bag (default=1) |
| leaf | Number of leaves for each tree (default=100) |
| shrinkage | Shrinkage, or learning rate (default=0.1) |
| tc | Number of threshold candidates for tree spliting. -1 to use all feature values (default=256) |
| mls | Min leaf support -- minimum % of docs each leaf has to contain (default=1) |

| Linear Regression-specific Parameter Name | Description and arguments |
| --- | --- |
| L2 | L2 regularization parameter (default=1.0E-10) |



# Examples
Examples can be found on the "How to Use" page of the Lemur Project ("https://sourceforge.net/p/lemur/wiki/RankLib%20How%20to%20use/").


