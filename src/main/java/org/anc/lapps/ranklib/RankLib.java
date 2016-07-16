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
import java.util.Set;
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

        // If the Input discriminator is ERROR, return the Data as is.
        if (Discriminators.Uri.ERROR.equals(discriminator))
        {
            return input;
        }

        // If the Input discriminator is not GET, return a wrapped Error with an appropriate message.
        if (!Discriminators.Uri.GET.equals(discriminator))
        {
            String errorData = generateError("Invalid discriminator.\nExpected " + Discriminators.Uri.GET + "\nFound " + discriminator);
            logger.error(errorData);
            return errorData;
        }

        // Output an error if no parameters are given
        if (data.getPayload() == null)
        {
            String errorData = generateError("No input given.");
            logger.error(errorData);
            return errorData;
        }

        else
        {
            Path outputDirPath = null;
            Path inputDirPath = null;
            try
            {
                outputDirPath = Files.createTempDirectory("output");
                inputDirPath = Files.createTempDirectory("input");
            }
            catch (IOException e) {  }

            // Get the parameters
            String params = convertParameters(data, outputDirPath, inputDirPath);
            String[] paramsArray;

            // Split the parameters into an array
            try { paramsArray = params.split("\\s+"); }
            catch (PatternSyntaxException ex)
            {
                String errorData = generateError("Error in parameter syntax.");
                logger.error(errorData);
                return errorData;
            }

            // Create a stream to hold the output
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            // Save the old System.out
            PrintStream old = System.out;
            // Set the special stream
            System.setOut(ps);

            // Get Classpath parameter.
            String cp = null;
            if (data.getParameter("cp") != null)
            {
                cp = (String) data.getParameter("cp");
            }

            // If no classpath is given, call Evaluator's main function, which is the main
            // classpath of the jar file
            if ((cp == null) || cp.contains("Evaluator"))
            {
                Evaluator.main(paramsArray);
            }

            // If the classpath is the FeatureManager, run its main function
            else if (cp.contains("FeatureManager"))
            {
                FeatureManager.main(paramsArray);
            }

            // If the classpath is the FeatureManager, run its main function
            else if (cp.contains("Analyzer"))
            {
                Analyzer.main(paramsArray);
            }
            // If an unknown classpath is given, output a wrapped error
            else
            {
                // Set System.out back
                System.out.flush();
                System.setOut(old);

                String errorData = generateError("Classpath given not recognized:" + cp);
                logger.error(errorData);
                return errorData;
            }

            // Set System.out back
            System.out.flush();
            System.setOut(old);

            // Output results

            Map<String,String> outputPayload = new HashMap<>();



            try
            {
                File outputFolder = new File(outputDirPath.toString());
                File[] listOfFiles = outputFolder.listFiles();
                for (File listOfFile : listOfFiles)
                {
                    if (listOfFile.isFile())
                    {
                        String fileName = listOfFile.getName().replace(".txt", "");
                        String fileContent = readFile(listOfFile.getAbsolutePath());
                        if (fileName.equals(data.getParameter("save")))
                        {
                            fileContent = fileContent.replaceAll("##(.*)", "<!-- $1 -->");
                        }
                        outputPayload.put(fileName, fileContent);
                    }
                }
            }
            catch(IOException e) { }

            outputPayload.put("Printed", baos.toString());
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
     * @return A String representing the parameters of the Data object.
     */
    private String convertParameters(Data<String> data, Path outputDirPath, Path inputDirPath)
    {
        StringBuilder params = new StringBuilder();

        String program = (String) data.getParameter("program");

        // Get the payload and convert it back into a HashMap to get all input content from it.
        String payloadJson = data.getPayload();
        Map<String,String> payload = Serializer.parse(payloadJson, HashMap.class);

        if(program == null || program == "Evaluator")
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
                        String filePath = writeTempFile(param, inputContent, inputDirPath);
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
            // TODO: Find out what <slack> is
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
                    saveFilePath.append(outputDirPath).append("\\").append(data.getParameter(param)).append(".txt");
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


        else if(program == "Analyzer")
        {
            for (String key : payload.keySet()) {
                if (key == "baseline")
                {
                    String baselineContent = payload.get(key);
                    try
                    {
                        String filePath = writeTempFile(key, baselineContent, inputDirPath);
                        params.append(" -base ").append(filePath);
                    }
                    catch (IOException e) { }
                }
                else
                {
                    String fileContent = payload.get(key);
                    try
                    {
                        writeTempFile("performanceFile", fileContent, inputDirPath);
                    }
                    catch (IOException e) { }
                }
            }
            params.append(" -all ").append(inputDirPath).append("/");

            if(data.getParameter("np") != null)
            {
                params.append(" -np ").append(data.getParameter("np"));
            }
        }

        else if(program == "FeatureManager")
        {
            if(payload.get("input") != null)
            {
                String inputContent = payload.get("input");
                try
                {
                    String filePath = writeTempFile("input", inputContent, inputDirPath);
                    params.append(" -input ").append(filePath);
                }
                catch (IOException e) { }
            }

            params.append(" -output ").append(outputDirPath).append("/");

            if(data.getParameter("k") != null)
            {
                params.append(" -k ").append(data.getParameter("k"));
            }

            if(data.getParameter("tvs") != null)
            {
                params.append(" -tvs ").append(data.getParameter("tvs"));
            }

            if(data.getParameter("shuffle") != null)
            {
                params.append(" -shuffle ");
            }
        }

        return params.toString().substring(1);
    }

    public String readFile(String path) throws IOException {
        StringBuilder output = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line = br.readLine();
        while (line != null) {
            output.append(line).append("\n");
            line = br.readLine();
        }

        return output.toString();
    }

    public String writeTempFile(String fileName, String fileTxt, Path dirPath) throws IOException {
        Path file = Files.createTempFile(dirPath, fileName, ".txt");
        PrintWriter writer = new PrintWriter(file.toFile(), "UTF-8");
        writer.print(fileTxt);
        writer.close();
        return file.toString();
    }

}