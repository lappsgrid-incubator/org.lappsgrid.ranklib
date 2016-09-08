# Analyze Function

This function can be used to analyze performance files with a baseline.

## Input Keys

These are the possible input keys that can be given to the function.

| Key | Description |
| --- | --- |
| baseline | Performance file for the baseline |
| file[k] | Performance file. The baseline should not be given twice. Any number of performance files can be given with the prefix "file" followed by a number: eg: file1, file2, file3, etc... |

## General Parameter (Optional)

Additionally, the function can take the parameter below, which is OPTIONAL and takes its argument as a string.

| Parameter Name | Description and arguments |
| --- | --- |
| np | Number of permutation (Fisher randomization test) (default=10000) |