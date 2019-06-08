package pl.edu.agh.ki.mob.onto;

import android.content.res.Resources;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.PrintUtil;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.vocabulary.RDF;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ClassHierarchy {
    // Constants
    //////////////////////////////////

    // Static variables
    //////////////////////////////////

    // Instance variables
    //////////////////////////////////

    protected OntModel m_model;
    private Map<AnonId, String> m_anonIDs = new HashMap<AnonId, String>();
    private int m_anonCount = 0;


    // Constructors
    //////////////////////////////////

    // External signature methods
    //////////////////////////////////

    /**
     * Show the sub-class hierarchy encoded by the given model
     */
    public void showHierarchy(PrintStream out, OntModel m) {
        // create an iterator over the root classes that are not anonymous class expressions
        Iterator<OntClass> i = m.listHierarchyRootClasses()
                .filterDrop(new Filter<OntClass>() {
                    @Override
                    public boolean accept(OntClass ontClass) {
                        return ontClass.isAnon();
                    }
                });

        while (i.hasNext()) {
            showClass(out, i.next(), new ArrayList<OntClass>(), 0);
        }
    }


    // Internal implementation methods
    //////////////////////////////////

    /**
     * Present a class, then recurse down to the sub-classes.
     * Use occurs check to prevent getting stuck in a loop
     */
    protected void showClass(PrintStream out, OntClass cls, List<OntClass> occurs, int depth) {
        renderClassDescription(out, cls, depth);
        out.println();

        // recurse to the next level down
        if (cls.canAs(OntClass.class) && !occurs.contains(cls)) {
            for (Iterator<OntClass> i = cls.listSubClasses(true); i.hasNext(); ) {
                OntClass sub = i.next();

                // we push this expression on the occurs list before we recurse
                occurs.add(cls);
                showClass(out, sub, occurs, depth + 1);
                occurs.remove(cls);
            }
        }
    }


    /**
     * <p>Render a description of the given class to the given output stream.</p>
     *
     * @param out A print stream to write to
     * @param c   The class to render
     */
    public void renderClassDescription(PrintStream out, OntClass c, int depth) {
        indent(out, depth);

        if (c.isRestriction()) {
            renderRestriction(out, c.as(Restriction.class));
        } else {
            if (!c.isAnon()) {
                out.print("Class ");
                renderURI(out, c.getModel(), c.getURI());
                out.print(' ');
            } else {
                renderAnonymous(out, c, "class");
            }
        }
    }

    /**
     * <p>Handle the case of rendering a restriction.</p>
     *
     * @param out The print stream to write to
     * @param r   The restriction to render
     */
    protected void renderRestriction(PrintStream out, Restriction r) {
        if (!r.isAnon()) {
            out.print("Restriction ");
            renderURI(out, r.getModel(), r.getURI());
        } else {
            renderAnonymous(out, r, "restriction");
        }

        out.print(" on property ");
        renderURI(out, r.getModel(), r.getOnProperty().getURI());
    }

    /**
     * Render a URI
     */
    protected void renderURI(PrintStream out, PrefixMapping prefixes, String uri) {
        out.print(prefixes.shortForm(uri));
    }

    /**
     * Render an anonymous class or restriction
     */
    protected void renderAnonymous(PrintStream out, Resource anon, String name) {
        String anonID = m_anonIDs.get(anon.getId());
        if (anonID == null) {
            anonID = "a-" + m_anonCount++;
            m_anonIDs.put(anon.getId(), anonID);
        }

        out.print("Anonymous ");
        out.print(name);
        out.print(" with ID ");
        out.print(anonID);
    }

    /**
     * Generate the indentation
     */
    protected void indent(PrintStream out, int depth) {
        for (int i = 0; i < depth; i++) {
            out.print("  ");
        }
    }

    //==============================================================================
    // Inner class definitions
    //==============================================================================

    public static void show(Resources resources) {
        new ClassHierarchy().showHierarchy(System.out, OntologyUtils.readOntology(resources));
    }

    public static void test(Resources resources) {

        Model schema = ModelFactory.createDefaultModel();
        schema.read(resources.openRawResource(R.raw.owldemoschema), "");

        Model data = ModelFactory.createDefaultModel();
        schema.read(resources.openRawResource(R.raw.owldemodata), "");

        Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        reasoner = reasoner.bindSchema(schema);
        InfModel infmodel = ModelFactory.createInfModel(reasoner, data);

        Resource nForce = infmodel.getResource("urn:x-hp:eg/nForce");
        System.out.println("nForce *:");
        printStatements(infmodel, nForce, null, null);

        Resource gamingComputer = infmodel.getResource("urn:x-hp:eg/GamingComputer");
        Resource whiteBox = infmodel.getResource("urn:x-hp:eg/whiteBoxZX");
        if (infmodel.contains(whiteBox, RDF.type, gamingComputer)) {
            System.out.println("White box recognized as gaming computer");
        } else {
            System.out.println("Failed to recognize white box correctly");
        }
    }

    private static void printStatements(Model m, Resource s, Property p, Resource o) {
        for (StmtIterator i = m.listStatements(s, p, o); i.hasNext(); ) {
            Statement stmt = i.nextStatement();
            System.out.println(" - " + PrintUtil.print(stmt));
        }
    }
}
