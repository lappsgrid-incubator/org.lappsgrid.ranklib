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

public class RankLib implements ProcessingService {

    /**
     * The Json String required by getMetadata()
     */
    private String metadata;
    private static final Logger logger = LoggerFactory.getLogger(RankLib.class);

    public RankLib() {

        metadata = generateMetadata();
    }

    private String generateMetadata() {

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
    public String execute(String input) {
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

        // Create a stream to hold the output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        // Save the old System.out
        PrintStream old = System.out;
        // Set the special stream
        System.setOut(ps);

        // Output an error if no parameters are given
        if(data.getParameter("params") == null) {
            // Set System.out back
            System.out.flush();
            System.setOut(old);
            // Wrap and output the error
            String errorData = generateError("No parameters given.");
            logger.error(errorData);
            return errorData;
        }

        else {
            // Get the parameters
            String params = (String) data.getParameter("params");
            String[] paramsArray;

            // Split the parameters into an array
            try {
                paramsArray = params.split("\\s+");
            } catch (PatternSyntaxException ex) {
                // Set System.out back
                System.out.flush();
                System.setOut(old);
                String errorData = generateError("Error in processing parameters.");
                logger.error(errorData);
                return errorData;
            }

            // If no classpath is given, call Evaluator's main function, which is the main
            // classpath of the jar file
            if(data.getParameter("cp") == null)
                Evaluator.main(paramsArray);

                // If a classpath is given, get the classpath string
            else {
                String cp = (String) data.getParameter("cp");

                // If the classpath is the FeatureManager, run its main function
                if(cp.contains("FeatureManager"))
                    FeatureManager.main(paramsArray);

                    // If the classpath is the FeatureManager, run its main function
                else if(cp.contains("Analyzer"))
                    Analyzer.main(paramsArray);

                    // If an unknown classpath is given, output a wrapped error
                else {
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
}