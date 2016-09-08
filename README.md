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

# Examples
Examples can be found on the ["How to Use" page](https://sourceforge.net/p/lemur/wiki/RankLib%20How%20to%20use/) of the Lemur Project.
