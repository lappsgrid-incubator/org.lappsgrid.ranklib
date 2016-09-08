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


