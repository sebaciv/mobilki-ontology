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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.vocabulary.RDF;

import java.io.InputStream;

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

    public static OntModel readOntology(Resources resources) {
        OntModel ontoSchema = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
        InputStream resource = resources.openRawResource(R.raw.ontology2);
        ontoSchema.read(resource, "");
        return ontoSchema;
    }

    public static void classificationPOC(Resources resources) {
        OntModel ontoSchema = readOntology(resources);

        OntClass humanClass = ontoSchema.getOntClass(HUMAN_CLASS);
        Individual individual = humanClass.createIndividual();

        ObjectProperty locationProperty = ontoSchema.createObjectProperty(LOCATION_PROP);

        ObjectProperty notActiveProperty = ontoSchema.createObjectProperty(NOT_ACTIVE_PROP);
        ObjectProperty movementProperty = ontoSchema.createObjectProperty(MOVEMENT_PROP);
        DatatypeProperty tempProperty = ontoSchema.getDatatypeProperty(TEMPERATURE_PROP);
        DatatypeProperty velocityProperty = ontoSchema.getDatatypeProperty(VELOCITY_PROP);

        individual.addProperty(locationProperty, new ResourceImpl(OUTSIDE_LOCATION_CLASS));
        individual.addProperty(notActiveProperty, new ResourceImpl(PHONE_CLASS));
        individual.addProperty(movementProperty, new ResourceImpl(SEIZURES_MOVEMENT_CLASS));
        individual.addLiteral(tempProperty, 50);
        individual.addLiteral(velocityProperty, 1);

        Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        reasoner = reasoner.bindSchema(ontoSchema);
        InfModel infmodel = ModelFactory.createInfModel(reasoner, individual.getModel());

        individual.getOntModel().write(System.out);

        Resource standing = infmodel.getResource(FALLEN_HEATSTROKE_HUMAN_CLASS);
        Resource fallen = infmodel.getResource(FALLEN_HUMAN_CLASS);

        if (infmodel.contains(individual, RDF.type, fallen)) {
            System.out.println("Fallen!");
        } else {
            System.out.println("Well..");
        }
    }
}
