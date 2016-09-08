# AdaRank-specific Parameters

These parameters are specific to the AdaRank ranker and can be added to the general parameters of the train function.

| AdaRank-specific Parameter Name | Description and arguments |
| --- | --- |
| round | The number of rounds to train (default=500) |
| noeq | Train without enqueuing too-strong features (default=unspecified). This is a boolean parameter: it will be set if it has any argument. If you wish to not have it set, simply leave it null |
| tolerance | Tolerance between two consecutive rounds of learning (default=0.002) |
| max | The maximum number of times can a feature be consecutively selected without changing performance (default=5) |
