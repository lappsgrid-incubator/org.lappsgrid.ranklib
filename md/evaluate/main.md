# Evaluate Function

This function can be used to test previously saved models.

## Input Keys

These are the possible input keys that can be given to the function.

| Key | Description |
| --- | --- |
| load | The model to load |
| load[k] | (Optional) Multiple models can be loaded to specify models from multiple folds (in increasing order). The input key to these files should be "load" followed by a number, eg: load1, load2, load3... |
| test | Test data to evaluate the model(s). Specify either test data or rank data, but not both. |
| rank | Rank the samples in the specified file. Specify either test data or rank data, but not both. |
| qrel | (Optional) TREC-style relevane judgement file. It only affects MAP and NDCG (default=unspecified) |

## General Parameters (Optional)

Additionally, the function can take the parameters listed in the table below. All parameters are OPTIONAL and take their arguments as strings.

| Parameter Name | Description and arguments |
| --- | --- |
| metric2T | Metric to evaluate on the test data. Supported: MAP, NDCG@k, DCG@k, P@k, RR@k, ERR@k (default=ERR@10) |
| gmax | Highest judged relevance label. It affecs the calculation of ERR (default =4, i.e. 5-point scale {0,1,2,3,4}) |
| score | Title of storing ranker's score for each object being ranked (has to be used with rank) |
| idv | Title to save model performance (in test metric) on individual ranked lists (has to be used with test) |
| norm | (Optional) Normalize all feature vectors (default=no-normalization). Method can be:
| | sum: normalize each feature by the sum of all its values |
| | zscore: normalize each feature by its mean/standard deviation |
| | linear: normalize each feature by its min/max values |