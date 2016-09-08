# Random Forests-specific Parameters

These parameters are specific to the Random Forests ranker and can be added to the general parameters of the train function.

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