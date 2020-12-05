package partiql.s3.client;

import com.amazon.ion.IonDatagram;
import com.amazon.ion.IonReader;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonWriter;
import com.amazon.ion.system.IonReaderBuilder;
import com.amazon.ion.system.IonSystemBuilder;
import com.amazon.ion.system.IonTextWriterBuilder;

import org.partiql.lang.CompilerPipeline;
import org.partiql.lang.eval.Bindings;
import org.partiql.lang.eval.EvaluationSession;
import org.partiql.lang.eval.ExprValue;
import org.partiql.lang.eval.Expression;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;

public class PartiQLClient {

    public static final void main(String[] args) {
        // Setup this variables before running the example:
        String input_location = "/tmp/partiqldata/s3_example.json"; // args[0];
        String query = "SELECT doc.name, doc.address FROM myS3Document doc WHERE doc.age < 30"; // args[1];

        // Initializes the ion system used by PartiQL
        final IonSystem ion = IonSystemBuilder.standard().build();

        // CompilerPipeline is the main entry point for the PartiQL lib giving you access to the compiler
        // and value factories
        final CompilerPipeline pipeline = CompilerPipeline.standard(ion);

        // Compiles the query, the resulting expression can be re-used to query multiple data sets
        final Expression selectAndFilter = pipeline.compile(query);

        final File f = new File(input_location);
		try (
                // We are using ion-java to parse the JSON data as PartiQL comes with an embedded value factory for
                // Ion data and Ion being a superset of JSON any JSON data is also Ion data
                // http://amzn.github.io/ion-docs/
                // https://github.com/amzn/ion-java
        		final InputStream is = new FileInputStream(f);
                final IonReader ionReader = IonReaderBuilder.standard().build(is);

                // We are using ion-java again to dump the PartiQL query result as JSON
                final IonWriter resultWriter = IonTextWriterBuilder.json().build((Appendable) System.out);
        ) {
            // Parses all data from the S3 bucket into the Ion DOM
            final IonDatagram values = ion.getLoader().load(ionReader);

            // Evaluation session encapsulates all information to evaluate a PartiQL expression, including the
            // global bindings
            final EvaluationSession session = EvaluationSession.builder()
            		// We implement the Bindings interface using a lambda. Bindings are used to map names into values,
                    // in this case we are binding the data from the S3 bucket into the "myS3Document" name
                    .globals(
                            Bindings.<ExprValue>lazyBindingsBuilder()
                                    .addBinding("myS3Document", () -> pipeline.getValueFactory().newFromIonValue(values))
                                    .build()
                    )
                    .build();

            // Executes the query in the session that's encapsulating the JSON data
            final ExprValue selectAndFilterResult = selectAndFilter.eval(session);

            // Uses ion-java to dump the result as JSON. It's possible to build your own writer and dump the ExprValue
            // as any format you want.
            selectAndFilterResult.getIonValue().writeTo(resultWriter);
            // result as JSON below
            // [{"name":"person_2"},{"name":"person_3","address":{"number":555,"street":"1st street","city":"Seattle"}}]
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
