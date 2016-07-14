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
import org.lappsgrid.serialization.lif.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
        if (Discriminators.Uri.ERROR.equals(discriminator)) {
            return input;
        }

        // If the Input discriminator is not GET, return a wrapped Error with an appropriate message.
        if (!Discriminators.Uri.GET.equals(discriminator))
        {
            String errorData = generateError("Invalid discriminator.\nExpected " + Discriminators.Uri.GET + "\nFound " + discriminator);
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

        // Output an error if no parameters are given
        if (data.getPayload() == null)
        {
            // Set System.out back
            System.out.flush();
            System.setOut(old);
            // Wrap and output the error
            String errorData = generateError("No parameters given.");
            logger.error(errorData);
            return errorData;
        }

        else
            {
            // Get the parameters
            String params = data.getPayload();
            String[] paramsArray;

            // Split the parameters into an array
            try {
                paramsArray = params.split("\\s+");
            } catch (PatternSyntaxException ex) {
                // Set System.out back
                System.out.flush();
                System.setOut(old);
                String errorData = generateError("Error in parameter syntax.");
                logger.error(errorData);
                return errorData;
            }

            // Get Classpath parameter.
            String cp = null;
            if (data.getParameter("cp") != null) {
                cp = (String) data.getParameter("cp");
            }

            // If no classpath is given, call Evaluator's main function, which is the main
            // classpath of the jar file
            if ((cp == null) || cp.contains("Evaluator")) {
                Evaluator.main(paramsArray);
            }

            // If the classpath is the FeatureManager, run its main function
            else if (cp.contains("FeatureManager")) {
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
        }

        // Set System.out back
        System.out.flush();
        System.setOut(old);

        // Output results
        Container container = new Container();
        container.setText(baos.toString());
        Data<Container> output = new Data<>(Discriminators.Uri.LAPPS, container);
        return output.asPrettyJson();
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
    private String convertParameters(Data<String> data)
    {
        StringBuilder params = new StringBuilder();

        // For each of the possible parameters, when it is given by the user, append to the output String
        // the name of the parameter and its value.

        // Parameters for the main Evaluator class start here.
        if(data.getParameter("train") != null)
        {
            params.append(" -train ").append(data.getParameter("train"));
        }

        if(data.getParameter("ranker") != null)
        {
            params.append(" -ranker ").append(data.getParameter("ranker"));
        }

        if(data.getParameter("feature") != null)
        {
            params.append(" -feature ").append(data.getParameter("feature"));
        }

        if(data.getParameter("metric2t") != null)
        {
            params.append(" -metric2t ").append(data.getParameter("metric2t"));
        }

        if(data.getParameter("metric2T") != null)
        {
            params.append(" -metric2T ").append(data.getParameter("metric2T"));
        }

        if(data.getParameter("gmax") != null)
        {
            params.append(" -gmax ").append(data.getParameter("gmax"));
        }

        if(data.getParameter("qrel") != null)
        {
            params.append(" -qrel ").append(data.getParameter("qrel"));
        }

        if(data.getParameter("tts") != null)
        {
            params.append(" -tts ").append(data.getParameter("tts"));
        }

        if(data.getParameter("tvs") != null)
        {
            params.append(" -tvs ").append(data.getParameter("tvs"));
        }

        if(data.getParameter("kcv") != null)
        {
            params.append(" -kcv ").append(data.getParameter("kcv"));
        }

        if(data.getParameter("validate") != null)
        {
            params.append(" -validate ").append(data.getParameter("validate"));
        }

        if(data.getParameter("test") != null)
        {
            params.append(" -test ").append(data.getParameter("test"));
        }

        if(data.getParameter("norm") != null)
        {
            params.append(" -norm ").append(data.getParameter("norm"));
        }

        if(data.getParameter("save") != null)
        {
            params.append(" -save ").append(data.getParameter("save"));
        }

        if(data.getParameter("kcvmd") != null)
        {
            params.append(" -kcvmd ").append(data.getParameter("kcvmd"));
        }

        if(data.getParameter("kcvmn") != null)
        {
            params.append(" -kcvmn ").append(data.getParameter("kcvmn"));
        }

        if(data.getParameter("load") != null)
        {
            params.append(" -load ").append(data.getParameter("load"));
        }

        if(data.getParameter("idv") != null)
        {
            params.append(" -idv ").append(data.getParameter("idv"));
        }

        if(data.getParameter("rank") != null)
        {
            params.append(" -rank ").append(data.getParameter("rank"));
        }

        if(data.getParameter("score") != null)
        {
            params.append(" -score ").append(data.getParameter("score"));
        }

        if(data.getParameter("epoch") != null)
        {
            params.append(" -epoch ").append(data.getParameter("epoch"));
        }

        if(data.getParameter("layer") != null)
        {
            params.append(" -layer ").append(data.getParameter("layer"));
        }


        if(data.getParameter("node") != null)
        {
            params.append(" -node ").append(data.getParameter("node"));
        }


        if(data.getParameter("lr") != null)
        {
            params.append(" -lr ").append(data.getParameter("lr"));
        }

        if(data.getParameter("tc") != null)
        {
            params.append(" -tc ").append(data.getParameter("tc"));
        }

        if(data.getParameter("max") != null)
        {
            params.append(" -max ").append(data.getParameter("max"));
        }

        if(data.getParameter("r") != null)
        {
            params.append(" -r ").append(data.getParameter("r"));
        }

        if(data.getParameter("i") != null)
        {
            params.append(" -i ").append(data.getParameter("i"));
        }

        if(data.getParameter("round") != null)
        {
            params.append(" -round ").append(data.getParameter("round"));
        }

        if(data.getParameter("reg") != null)
        {
            params.append(" -reg ").append(data.getParameter("reg"));
        }

        if(data.getParameter("tolerance") != null)
        {
            params.append(" -tolerance ").append(data.getParameter("tolerance"));
        }

        if(data.getParameter("tree") != null)
        {
            params.append(" -tree ").append(data.getParameter("tree"));
        }

        if(data.getParameter("leaf") != null)
        {
            params.append(" -leaf ").append(data.getParameter("leaf"));
        }

        if(data.getParameter("shrinkage") != null)
        {
            params.append(" -shrinkage ").append(data.getParameter("shrinkage"));
        }

        if(data.getParameter("mls") != null)
        {
            params.append(" -mls ").append(data.getParameter("mls"));
        }

        if(data.getParameter("estop") != null)
        {
            params.append(" -estop ").append(data.getParameter("estop"));
        }

        if(data.getParameter("gcc") != null)
        {
            params.append(" -gcc ").append(data.getParameter("gcc"));
        }

        if(data.getParameter("bag") != null)
        {
            params.append(" -bag ").append(data.getParameter("bag"));
        }

        if(data.getParameter("srate") != null)
        {
            params.append(" -srate ").append(data.getParameter("srate"));
        }

        if(data.getParameter("frate") != null)
        {
            params.append(" -frate ").append(data.getParameter("frate"));
        }

        if(data.getParameter("rtype") != null) {
            params.append(" -rtype ").append(data.getParameter("rtype"));
        }

        if(data.getParameter("L2") != null)
        {
            params.append(" -L2 ").append(data.getParameter("L2"));
        }

        if(data.getParameter("thread") != null)
        {
            params.append(" -thread ").append(data.getParameter("thread"));
        }

        // Exceptions are boolean parameters which only need the name
        if(data.getParameter("sparse") != null)
        {
            params.append(" -sparse ");
        }

        if(data.getParameter("silent") != null)
        {
            params.append(" -silent ");
        }

        if(data.getParameter("noeg") != null)
        {
            params.append(" -noeg ");
        }


        // Parameters for the Analyzer class start here
        if(data.getParameter("all") != null) {
            params.append(" -all ").append(data.getParameter("all"));
        }

        if(data.getParameter("base") != null)
        {
            params.append(" -base ").append(data.getParameter("base"));
        }

        if(data.getParameter("np") != null)
        {
            params.append(" -np ").append(data.getParameter("np"));
        }

        // Parameters for the FeatureManager class start here
        if(data.getParameter("input") != null) {
            params.append(" -input ").append(data.getParameter("input"));
        }

        if(data.getParameter("k") != null)
        {
            params.append(" -k ").append(data.getParameter("k"));
        }

        if(data.getParameter("tvs") != null)
        {
            params.append(" -tvs ").append(data.getParameter("tvs"));
        }

        if(data.getParameter("output") != null)
        {
            params.append(" -output ").append(data.getParameter("tvs"));
        }

        if(data.getParameter("shuffle") != null)
        {
            params.append(" -shuffle ");
        }
        
        return params.toString();
    }

}