package pl.edu.agh.ki.mob.onto;

import android.content.res.Resources;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.vocabulary.RDF;

import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

public class OntologyUtils {
    private static final String URL_BASE = "http://www.semanticweb.org/mdz/ontologies/2019/5/untitled-ontology-2#";

    public static final String HUMAN_CLASS = URL_BASE + "Human";
    public static final String FALLEN_HUMAN_CLASS = URL_BASE + "FallenHuman";
    public static final String STANDING_HUMAN_CLASS = URL_BASE + "StandingHuman";
    public static final String FALLEN_EPILEPSY_HUMAN_CLASS = URL_BASE + "FallenHumanEpilepsy";
    public static final String FALLEN_HEATSTROKE_HUMAN_CLASS = URL_BASE + "FallenHumanHeatstroke";

    public static final String PHONE_CLASS = URL_BASE + "Phone";

    public static final String OUTSIDE_LOCATION_CLASS = URL_BASE + "OutsideLocation";
    public static final String HOME_LOCATION_CLASS = URL_BASE + "HomeLocation";
    public static final String HOSPITAL_LOCATION_CLASS = URL_BASE + "HospitalLocation";

    public static final String NO_MOVEMENT_CLASS = URL_BASE + "NoMovement";
    public static final String SEIZURES_MOVEMENT_CLASS = URL_BASE + "Seizures";

    public static final String LOCATION_PROP = URL_BASE + "IsInLocation";
    public static final String MOVEMENT_PROP = URL_BASE + "HasMovementLevel";
    public static final String NOT_ACTIVE_PROP = URL_BASE + "IsNotActivelyUsing";
    public static final String TEMPERATURE_PROP = URL_BASE + "Temperature";
    public static final String VELOCITY_PROP = URL_BASE + "Velocity";

    public enum HumanStatus {
        STANDING(STANDING_HUMAN_CLASS),
        FALLEN(FALLEN_HUMAN_CLASS),
        FALLEN_EPILEPSY(FALLEN_EPILEPSY_HUMAN_CLASS),
        FALLEN_HEATSTROKE(FALLEN_HEATSTROKE_HUMAN_CLASS);

        private final String url;

        HumanStatus(String url) {
            this.url = url;
        }

        String getUrl() {
            return url;
        }
    }

    public enum LocationType {
        OUTSIDE(OUTSIDE_LOCATION_CLASS),
        HOME(HOME_LOCATION_CLASS);

        private final String url;

        LocationType(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    public enum MovementType {
        NO_MOVEMENT(NO_MOVEMENT_CLASS),
        SEIZURES(SEIZURES_MOVEMENT_CLASS);

        private final String url;

        MovementType(String url) {
            this.url = url;
        }

        public String getUrl() {
            return url;
        }
    }

    public static OntModel readOntology(Resources resources) {
        OntModel ontoSchema = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
        InputStream resource = resources.openRawResource(R.raw.ontology2);
        ontoSchema.read(resource, "");
        return ontoSchema;
    }

    public static Optional<HumanStatus> classify(
            Resources resources,
            LocationType locationType,
            MovementType movementType,
            Optional<Double> temperature,
            Optional<Double> velocity
    ) {
        OntModel ontoSchema = readOntology(resources);

        OntClass humanClass = ontoSchema.getOntClass(HUMAN_CLASS);
        Individual individual = humanClass.createIndividual();

        ObjectProperty locationProperty = ontoSchema.createObjectProperty(LOCATION_PROP);

        ObjectProperty notActiveProperty = ontoSchema.createObjectProperty(NOT_ACTIVE_PROP);
        ObjectProperty movementProperty = ontoSchema.createObjectProperty(MOVEMENT_PROP);
        DatatypeProperty tempProperty = ontoSchema.getDatatypeProperty(TEMPERATURE_PROP);
        DatatypeProperty velocityProperty = ontoSchema.getDatatypeProperty(VELOCITY_PROP);

        individual.addProperty(locationProperty, new ResourceImpl(locationType.getUrl()));
        individual.addProperty(notActiveProperty, new ResourceImpl(PHONE_CLASS));
        individual.addProperty(movementProperty, new ResourceImpl(movementType.getUrl()));
        temperature.ifPresent(value -> individual.addLiteral(tempProperty, value.intValue()));
        velocity.ifPresent(value -> individual.addLiteral(velocityProperty, value.intValue()));

        Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        reasoner = reasoner.bindSchema(ontoSchema);
        InfModel infmodel = ModelFactory.createInfModel(reasoner, individual.getModel());

        individual.getOntModel().write(System.out);

        Optional<HumanStatus> result = Stream.of(HumanStatus.FALLEN_HEATSTROKE, HumanStatus.FALLEN_EPILEPSY, HumanStatus.FALLEN, HumanStatus.STANDING)
                .filter(status -> infmodel.contains(individual, RDF.type, infmodel.getResource(status.getUrl())))
                .findFirst();

        if (result.isPresent()) {
            System.out.println("Classified as" + result.get().name());
        } else {
            System.out.println("Cannot classify!");
        }
        return result;
    }
}
