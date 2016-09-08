# Manage Function (Feature Manager)

This function can be used to manage features.

## Input Keys

This function only takes one input key, named "input".

| Key | Description |
| --- | --- |
| input | The source data (ranked lists) |

## General Parameters (Optional)

Additionally, the function can take the parameters listed in the table below. All parameters are OPTIONAL and take their arguments as strings.

NOTE: If both -shuffle and -k are specified, the input data will be shuffled and then sequentially partitioned.

| Parameter Name | Description and arguments |
| --- | --- |
| shuffle | Create a copy of the input file in which the ordering of all ranked lists (e.g. queries) is randomized(the order among objects (e.g. documents) within each ranked list is certainly unchanged). This is a boolean parameter: it will be set if it has any argument. If you wish to not have it set, simply leave it null |
| k | The number of folds. k-fold partitioning (sequential split) |
| tvs | Train-validation split. Takes a value x between 0 and 1. Use this to set train-validation split to be (x)(1.0-x) |