# Train Function

This function takes input with the following keys.

## Input Keys

These are the possible input keys that can be given to the function.

| Key | Description |
| --- | --- |
| train | Training data |
| feature | (Optional) Feature description file: list features to be considered by the learner, each on a separate line (default=all features will be used) |
| qrel | (Optional) TREC-style relevance judgment file. It only affects MAP and NDCG (default=unspecified) |
| validate | (Optional) Specify if you want to tune your system on the validation data (default=unspecified). If specified, the final model will be the one that performs best on the validation data |
| test | (Optional) Specify if you want to evaluate the trained model on this data (default=unspecified) |

## Parameters

Additionally, the function takes the parameters listed in the table below. All parameters take their arguments as strings.

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
