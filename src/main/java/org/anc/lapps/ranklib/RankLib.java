package org.anc.lapps.ranklib;

import ciir.umass.edu.eval.Analyzer;
import ciir.umass.edu.eval.Evaluator;
import ciir.umass.edu.features.FeatureManager;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

/**
 * @author Alexandru Mahmoud
 */

public class RankLib implements ProcessingService
{

    /**
     * The Json String required by getMetadata()
     */
    private String metadata;
    private static final Logger logger = LoggerFactory.getLogger(RankLib.class);
    private ArrayList<File> allFiles = new ArrayList<>();

    public RankLib() { metadata = generateMetadata(); }

    private String generateMetadata()
    {
        ServiceMetadata metadata = new ServiceMetadata();
        metadata.setName(this.getClass().getName());
        metadata.setDescription("RankLib from The Lemur Project");
        metadata.setVersion(Version.getVersion());
        metadata.setVendor("http://www.lappsgrid.org");
        metadata.setLicense(Discriminators.Uri.APACHE2);

        IOSpecification requires = new IOSpecification();
        requires.addFormat(Discriminators.Uri.GET);
        requires.setEncoding("UTF-8");

        IOSpecification produces = new IOSpecification();
        produces.addFormat(Discriminators.Uri.LAPPS);
        produces.addAnnotation(Discriminators.Uri.TOKEN);
        produces.setEncoding("UTF-8");

        metadata.setRequires(requires);
        metadata.setProduces(produces);

        Data<ServiceMetadata> data = new Data<>();
        data.setDiscriminator(Discriminators.Uri.META);
        data.setPayload(metadata);
        return data.asPrettyJson();
    }

    @Override
    /**
     * Returns a JSON string containing metadata describing the service. The
     * JSON <em>must</em> conform to the json-schema at
     * <a href="http://vocab.lappsgrid.org/schema/service-schema.json">http://vocab.lappsgrid.org/schema/service-schema.json</a>
     * (processing services) or
     * <a href="http://vocab.lappsgrid.org/schema/datasource-schema.json">http://vocab.lappsgrid.org/schema/datasource-schema.json</a>
     * (datasources).
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * Entry point for a Lappsgrid service.
     * <p>
     * Each service on the Lappsgrid will accept {@code org.lappsgrid.serialization.Data} object
     * and return a {@code Data} object with a {@code org.lappsgrid.serialization.lif.Container}
     * payload.
     * <p>
     * Errors and exceptions that occur during processing should be wrapped in a {@code Data}
     * object with the discriminator set to http://vocab.lappsgrid.org/ns/error
     * <p>
     * See <a href="https://lapp.github.io/org.lappsgrid.serialization/index.html?org/lappsgrid/serialization/Data.html>org.lappsgrid.serialization.Data</a><br />
     * See <a href="https://lapp.github.io/org.lappsgrid.serialization/index.html?org/lappsgrid/serialization/lif/Container.html>org.lappsgrid.serialization.lif.Container</a><br />
     *
     * @param input A JSON string representing a Data object
     * @return A JSON string containing a Data object with a Container payload.
     */
    @Override
    public String execute(String input)
    {
        // Parse the JSON string into a Data object, and extract its discriminator.
        Data<String> data = Serializer.parse(input, Data.class);
        String discriminator = data.getDiscriminator();

        // If the Input discriminator is ERROR, return the Data as is, since it's already a wrapped error.
        if (Discriminators.Uri.ERROR.equals(discriminator))
        {
            return input;
        }

        // If the Input discriminator is not GET, return a wrapped Error with an appropriate message.
        else if (!Discriminators.Uri.GET.equals(discriminator))
        {
            String errorData = generateError("Invalid discriminator.\nExpected " + Discriminators.Uri.GET + "\nFound " + discriminator);
            logger.error(errorData);
            return errorData;
        }

        // Output an error if no payload is given, since an input is required to run the program
        if (data.getPayload() == null)
        {
            String errorData = generateError("No input given.");
            logger.error(errorData);
            return errorData;
        }

        // Else (if a payload is given), process the input
        else
        {
            // Create temporary directories to hold input and output. This is needed because
            // the RankLib methods need directories for most of their processing, so the input
            // will be given within files in a directory, and the output will be read from files
            // in the output directory.
            Path outputDirPath = null;
            Path inputDirPath = null;
            try
            {
                outputDirPath = Files.createTempDirectory("output");
                outputDirPath.toFile().deleteOnExit();
                inputDirPath = Files.createTempDirectory("input");
                inputDirPath.toFile().deleteOnExit();
            }
            catch (IOException e) {  }

            // Call the method that converts the parameters to the format that they would
            // be in when given from command-line.
            String params = convertParameters(data, outputDirPath, inputDirPath);
            String[] paramsArray;

            // Split the parameters into an array, which will be given as the args[] argument
            // to the main methods of RankLib.
            try { paramsArray = params.split("\\s+"); }
            catch (PatternSyntaxException ex)
            {
                String errorData = generateError("Error in parameter syntax.");
                logger.error(errorData);
                return errorData;
            }

            // Create a stream to hold the output from System.out.println. This is necessary
            // because when running, the program will print things from many RankLib classes and
            // methods. So the printed output will be "caught" and saved to output.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            // Save the old System.out PrintStream, to reset at the end of the program.
            PrintStream oldPrintStream = System.out;
            // Set the special stream as the out stream
            System.setOut(ps);

            // Get the function parameter, which determines which of the Analyzer, the Evaluator, or
            // the FeaturesManager from RankLib one wants to use.
            String function = (String) data.getParameter("function");

            // If no function parameter is given, process as if the Evaluator was called, since it
            // is the default running class of RankLib. If either "Evaluate" or "Train" are given
            // process them together, since they are both called by the same Evaluator class.
            if(function == null || function.contains("Evaluat") || function.contains("Train"))
            {
                Evaluator.main(paramsArray);
            }

            // If the function parameter is the Manager, run its main function
            else if (function.contains("Manag"))
            {
                FeatureManager.main(paramsArray);
            }

            // If the function parameter is Analyze, run its main function
            else if (function.contains("Analyz"))
            {
                Analyzer.main(paramsArray);
            }

            // If an unknown function name is given, output a wrapped error
            else
            {
                // Set System.out back to the original PrintStream
                System.out.flush();
                System.setOut(oldPrintStream);

                String errorData = generateError("Classpath given not recognized:" + function);
                logger.error(errorData);
                return errorData;
            }

            // Set System.out back to the original PrintStream
            System.out.flush();
            System.setOut(oldPrintStream);

            // Make a Map to hold both the printed, and file outputs.
            Map<String,String> outputPayload = new HashMap<>();

            try
            {
                // Process all the files in the output folder, to return them
                // as part of the outputted Data object, and delete them from the
                // temporary directory
                File outputFolder = new File(outputDirPath.toString());
                File[] listOfFiles = outputFolder.listFiles();
                for (File file : listOfFiles)
                {
                    if (file.isFile())
                    {
                        // Add the file to the list of all files that will be used
                        // to remove the long file names in the printed output.
                        allFiles.add(file);
                        // Get the filename, to serve as the key in the Map object, and
                        // the content of the file to be put in the output
                        String fileName = file.getName().replace(".txt", "");
                        String fileContent = readFile(file.getAbsolutePath());

                        // This is to change the wrongly commented xml file that is output
                        // by the Evaluator class when running with the -save parameter. The
                        // model is in XML format but starts with comments starting with ##
                        // rather than in <!-- --> brackets.
                        if (fileName.equals(data.getParameter("save")))
                        {
                            fileContent = fileContent.replaceAll("##(.*)", "<!-- $1 -->");
                        }

                        outputPayload.put(fileName, fileContent);
                        file.deleteOnExit();
                    }
                }
            }
            catch(IOException e) { }

            String printedOutput = baos.toString();

            // Replace the long paths with the filename in the printed output. This is purely
            // for aesthetic reasons.
            for(File file : allFiles)
            {
                printedOutput = printedOutput.replace(file.getAbsolutePath(), file.getName());
            }

            // Also add the printed text caught from the out stream to the payload
            // with the "Printed" key
            outputPayload.put("Printed", printedOutput);

            // Parse the Map to Json, then put it as a payload to a Data object with a LAPPS
            // discriminator and return it as the final output
            String outputJson = Serializer.toJson(outputPayload);
            Data<String> output = new Data<>(Discriminators.Uri.LAPPS, outputJson);
            return output.asPrettyJson();
        }
    }


    /** This method takes an error message and returns it in a {@code Data}
     * object with the discriminator set to http://vocab.lappsgrid.org/ns/error
     *
     * @param message A string representing the error message
     * @return A JSON string containing a Data object with the message as a payload.
     */
    private String generateError(String message)
    {
        Data<String> data = new Data<>();
        data.setDiscriminator(Discriminators.Uri.ERROR);
        data.setPayload(message);
        return data.asPrettyJson();
    }

    /** This method takes in the input data and returns its parameters as an array of strings,
     * representing the parameters as they would be written to run the jar files from command-line,
     * to be given as input to the main classes.
     *
     * @param data A Data object
     * @param outputDirPath A Path to the output directory
     * @param inputDirPath A Path to the input directory
     * @return A String representing the parameters of the Data object.
     */
    private String convertParameters(Data<String> data, Path outputDirPath, Path inputDirPath)
    {
        StringBuilder params = new StringBuilder();

        // Get the name of the function, to know which parameters to process.
        String function = (String) data.getParameter("function");

        // Get the payload and convert it back into a HashMap to get all input content from it.
        String payloadJson = data.getPayload();
        Map<String,String> payload = Serializer.parse(payloadJson, HashMap.class);

        // If no function parameter is given, process as if the Evaluator was called, since it
        // is the default running class of RankLib. If either "Evaluate" or "Train" are given
        // process them together, since they are both called by the same Evaluator class.
        if(function == null || function.contains("Evaluat") || function.contains("Train"))
        {
            // These are the parameters from the Evaluator class that give input
            ArrayList<String> EvaluatorInputParams = new ArrayList();
            EvaluatorInputParams.add("train");
            EvaluatorInputParams.add("feature");
            EvaluatorInputParams.add("qrel");
            EvaluatorInputParams.add("validate");
            EvaluatorInputParams.add("test");
            EvaluatorInputParams.add("load");
            EvaluatorInputParams.add("rank");

            // For each possible parameter, check if it is set
            for(String param : EvaluatorInputParams)
            {
                // If the parameter is set, write its content to a temporary file and add the
                // parameter with its path to the output parameter. This will allow the RankLib
                // program to read the content and process it as usual.
                if(payload.get(param) != null)
                {
                    String inputContent = payload.get(param);
                    try
                    {
                        Path filePath = writeTempFile(param, inputDirPath, inputContent);
                        params.append(" -").append(param).append(" ").append(filePath);
                    } catch (IOException e) { }
                }
            }

            // These are the parameters from the Evaluator class that are followed
            // by a String argument.
            ArrayList<String> EvaluatorStringParams = new ArrayList();
            EvaluatorStringParams.add("ranker");
            EvaluatorStringParams.add("metric2t");
            EvaluatorStringParams.add("gmax");
            EvaluatorStringParams.add("tvs");
            EvaluatorStringParams.add("tts");
            EvaluatorStringParams.add("metric2T");
            EvaluatorStringParams.add("norm");
            EvaluatorStringParams.add("kcv");
            EvaluatorStringParams.add("epoch");
            EvaluatorStringParams.add("layer");
            EvaluatorStringParams.add("node");
            EvaluatorStringParams.add("lr");
            EvaluatorStringParams.add("round");
            EvaluatorStringParams.add("tc");
            EvaluatorStringParams.add("noeq");
            EvaluatorStringParams.add("tolerance");
            EvaluatorStringParams.add("max");
            EvaluatorStringParams.add("r");
            EvaluatorStringParams.add("i");
            EvaluatorStringParams.add("reg");
            EvaluatorStringParams.add("tree");
            EvaluatorStringParams.add("leaf");
            EvaluatorStringParams.add("shrinkage");
            EvaluatorStringParams.add("tc");
            EvaluatorStringParams.add("mls");
            EvaluatorStringParams.add("estop");
            EvaluatorStringParams.add("bag");
            EvaluatorStringParams.add("srate");
            EvaluatorStringParams.add("frate");
            EvaluatorStringParams.add("L2");
            EvaluatorStringParams.add("rtype");

            // For each possible parameter, check if it is set
            for(String param : EvaluatorStringParams)
            {
                // If the parameter is set, add the parameter name and its argument to the output
                // String to be given to the RankLib program
                if(data.getParameter(param) != null)
                {
                    params.append(" -").append(param).append(" ").append(data.getParameter(param));
                }
            }

            // These are the parameters from the Evaluator class that are not
            // followed by any argument, which makes them boolean parameters
            ArrayList<String> EvaluatorBooleanParams = new ArrayList();
            EvaluatorBooleanParams.add("silent");
            EvaluatorBooleanParams.add("noeq");

            // For each possible parameter, check if it is set
            for(String param : EvaluatorBooleanParams)
            {
                // If the parameter is set, add the parameter name to the output String
                // to be given to the RankLib program. These parameters don't have arguments.
                if(data.getParameter(param) != null)
                {
                    params.append(" -").append(param).append(" ");
                }
            }

            // These are the parameters from the Evaluator class that are followed
            // by an output path
            ArrayList<String> EvaluatorOutputParams = new ArrayList();
            EvaluatorOutputParams.add("save");
            EvaluatorOutputParams.add("score");
            EvaluatorOutputParams.add("idv");

            // For each possible parameter, check if it is set
            for(String param : EvaluatorOutputParams)
            {
                // If the parameter is set, add the parameter with the path to the temporary
                // output directory followed by the filename to the parameters. This will allow
                // the RankLib program to write the content to these files which will get later
                // in the execute method to be added to the final output payload.
                if (data.getParameter(param) != null)
                {
                    StringBuilder saveFilePath = new StringBuilder();
                    saveFilePath.append(outputDirPath).append("/").append(data.getParameter(param)).append(".txt");
                    params.append(" -").append(param).append(" ").append(saveFilePath);
                }
            }

            // This parameter needs special processing because the original method takes two
            // separate parameters for the directory and the name of the files to be saved
            if (data.getParameter("saveCrossValidationModels") != null)
            {
                params.append(" -kcvmd ").append(outputDirPath).append("/");
                params.append(" -kcvmn ").append(data.getParameter("saveCrossValidationModels"));
            }
        }

        // If the function parameter is Analyze, process its possible parameters.
        else if(function.contains("Analyz"))
        {
            // Since the Analyzer only requires a baseline, and the rest of the files
            // given can have any key, we process all of the input and check for the baseline.
            for (String key : payload.keySet()) {

                // If the input file is the baseline, we take its content and save it to a
                // temporary file in the input directory, then add the -base parameter
                // with its path to the output parameter string.
                if (key.equals("baseline"))
                {
                    String baselineContent = payload.get(key);
                    try
                    {
                        Path filePath = writeTempFile(key, inputDirPath, baselineContent);
                        params.append(" -base ").append(filePath.getFileName());
                    }
                    catch (IOException e) { }
                }

                // For all other keys, we save the file to a temporary file in the input
                // directory. No parameter needs to be given for each file, since the entire
                // directory will be later given as a parameter.
                else
                {
                    String fileContent = payload.get(key);
                    try
                    {
                        writeTempFile(key, inputDirPath, fileContent);
                    }
                    catch (IOException e) { }
                }
            }

            // Add the -all parameter with the input directory path containing the baseline
            // as well as all the performance files.
            params.append(" -all ").append(inputDirPath).append("/");

            // If this parameter is set, also add it to the parameter string.
            if(data.getParameter("np") != null)
            {
                params.append(" -np ").append(data.getParameter("np"));
            }
        }

        // If the function parameter is Manage, process its possible parameters.
        else if(function.contains("Manage"))
        {
            // For the input file, get the content from the payload, then write it to a
            // temporary file of which path will be added with the -input parameter, to
            // the output parameters string.
            if(payload.get("input") != null)
            {
                String inputContent = payload.get("input");
                try
                {
                    Path filePath = writeTempFile("input", inputDirPath, inputContent);
                    params.append(" -input ").append(filePath);
                }
                catch (IOException e) { }
            }

            // Add the -output parameter with the path to the output directory to the
            // parameters string, since the program will write files in the directory
            // to then be processed and output in the final payload.
            params.append(" -output ").append(outputDirPath).append("/");

            // If either of the k or tvs parameters are set, also add them to the
            // parameter string.
            if(data.getParameter("k") != null)
            {
                params.append(" -k ").append(data.getParameter("k"));
            }

            if(data.getParameter("tvs") != null)
            {
                params.append(" -tvs ").append(data.getParameter("tvs"));
            }

            // If the shuffle parameter is set, add -shuffle to the parameters string.
            // This parameter does not require any additional input, as it is a boolean
            // parameter.
            if(data.getParameter("shuffle") != null)
            {
                params.append(" -shuffle ");
            }
        }

        // Return the resulting list of parameters to be processed as an array
        // and given as input to the RankLib classes' main methods.
        // The substring is used to remove the first space, since all parameters
        // are being set as " -param ". This will avoid it to be wrongly split
        // when getting the array of arguments.
        return params.toString().substring(1);
    }

    /** This method will read a text file from a path, and output its contents as a String.
     *
     * @param path The path to the text file that should be read
     * @return A String representing the contents of the text file.
     */
    public String readFile(String path) throws IOException {
        StringBuilder output = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line = br.readLine();
        while (line != null) {
            output.append(line).append("\r\n");
            line = br.readLine();
        }
        br.close();
        return output.toString();
    }

    /** This method creates a temporary text file at a certain directory, and writes
     * the given content into the file. The file will also be set to delete on exit.
     *
     * @param fileName The prefix for the temporary file to be created
     * @param dirPath The path to the directory in which the file should be created
     * @param fileTxt The text to be written in the file
     * @return A path to the temporary text file that was created
     */
    public Path writeTempFile(String fileName, Path dirPath, String fileTxt) throws IOException {
        Path filePath = Files.createTempFile(dirPath, fileName, ".txt");
        File file = filePath.toFile();
        // Add the file to the list of all files that will be used
        // to remove the long file names in the printed output.
        allFiles.add(file);
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.print(fileTxt);
        writer.close();
        file.deleteOnExit();
        return filePath;
    }


    /*
    public void deleteDirectory(File directory)
    {
        if(directory.isDirectory())
        {
            String files[] = directory.list();
            if(files.length == 0) { directory.delete(); }
            else
            {
                for(String fileName : files)
                {
                    File currentFile = new File(directory, fileName);
                    if(currentFile.isDirectory())
                    {
                        deleteDirectory(currentFile);
                    }
                    else
                    {
                        System.out.println(currentFile);
                        try {
                            Files.delete(currentFile.toPath());
                        } catch (IOException e) {
                        }
                    }
                }
                try {
                    Files.delete(directory.toPath());
                } catch (IOException e) {
                }
            }
        }
    }
    */

}