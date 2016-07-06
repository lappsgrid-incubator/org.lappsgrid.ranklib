package org.anc.lapps.ranklib;

import ciir.umass.edu.eval.Analyzer;
import ciir.umass.edu.eval.Evaluator;
import ciir.umass.edu.features.*;
import ciir.umass.edu.learning.*;
import ciir.umass.edu.learning.boosting.AdaRank;
import ciir.umass.edu.learning.boosting.RankBoost;
import ciir.umass.edu.learning.neuralnet.ListNet;
import ciir.umass.edu.learning.neuralnet.Neuron;
import ciir.umass.edu.learning.neuralnet.RankNet;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.learning.tree.RFRanker;
import ciir.umass.edu.metric.ERRScorer;
import ciir.umass.edu.metric.MetricScorer;
import ciir.umass.edu.metric.MetricScorerFactory;
import ciir.umass.edu.utilities.MyThreadPool;
import ciir.umass.edu.utilities.RankLibError;
import ciir.umass.edu.utilities.SimpleMath;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static ciir.umass.edu.eval.Evaluator.modelFile;
import static ciir.umass.edu.eval.Evaluator.qrelFile;
import static ciir.umass.edu.eval.Evaluator.useSparseRepresentation;

/**
 * @author Alexandru Mahmoud
 */

public class RankLib implements ProcessingService {

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

        IOSpecification produces = new IOSpecification();
        produces.addFormat(Discriminators.Uri.LAPPS);
        produces.addAnnotation(Discriminators.Uri.TOKEN);

        metadata.setRequires(requires);
        metadata.setProduces(produces);

        Data<ServiceMetadata> data = new Data<>(Discriminators.Uri.META, metadata);
        return data.asPrettyJson();

    }

    @Override
    /**
     * getMetadata simply returns metadata populated in the constructor
     */
    public String getMetadata() {
        return metadata;
    }


    @Override
    public String execute(String input) {

        Data<String> data = Serializer.parse(input, Data.class);
        String discriminator = data.getDiscriminator();
        if (Discriminators.Uri.ERROR.equals(discriminator))
        {
            return input;
        }

        if (!Discriminators.Uri.GET.equals(discriminator))
        {
            return generateError("Invalid discriminator.\nExpected " + Discriminators.Uri.GET + "\nFound " + discriminator);
        }

        // Create a stream to hold the output
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        // Save the old System.out!
        PrintStream old = System.out;
        // Tell Java to use special stream
        System.setOut(ps);

        if(data.getParameter("params") == null) {
            // Set System.out back
            System.out.flush();
            System.setOut(old);
            return generateError("No parameters given.");
        }

        else {


            String params = (String) data.getParameter("params");
            String[] paramsArray;

            try {
                paramsArray = params.split("\\s+");
            } catch (PatternSyntaxException ex) {
                // Set System.out back
                System.out.flush();
                System.setOut(old);
                return generateError("Error in processing parameters.");
            }

            if(data.getParameter("cp") == null)
                Evaluator.main(paramsArray);

            else {
                String cp = (String) data.getParameter("cp");

                if(cp.contains("FeatureManager"))
                    FeatureManager.main(paramsArray);

                else if(cp.contains("Analyzer"))
                    Analyzer.main(paramsArray);

                else {
                    // Set System.out back
                    System.out.flush();
                    System.setOut(old);

                    return generateError("Classpath given not recognized:" + cp);
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


    private String generateError(String message)
    {
        Data<String> data = new Data<>();
        data.setDiscriminator(Discriminators.Uri.ERROR);
        data.setPayload(message);
        return data.asPrettyJson();
    }

}
