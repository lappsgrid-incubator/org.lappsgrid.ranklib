# {MART, LambdaMART}-specific Parameters

These parameters are specific to the MART and the LambdaMART rankers and can be added to the general parameters of the train function.

| {MART, LambdaMART}-specific Parameter Name | Description and arguments |
| --- | --- |
| tree  | Number of trees (default=1000) |
| leaf | Number of leaves for each tree (default=10) |
| shrinkage | Shrinkage, or learning rate (default=0.1) |
| tc | Number of threshold candidates for tree spliting. -1 to use all feature values (default=256) |
| mls | Min leaf support -- minimum % of docs each leaf has to contain (default=1) |
| estop | Stop early when no improvement is observed on validaton data in e consecutive rounds (default=100) |