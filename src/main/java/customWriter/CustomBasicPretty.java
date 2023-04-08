package customWriter;

import org.apache.jena.rdf.model.*;

import java.io.PrintWriter;

/**
 * similar to {@link CustomBasic}. Prints the plain result with the rdf:Description replaced
 *
 * @author mz
 */
public class CustomBasicPretty extends CustomBasic {

    @Override
    protected void writeRDFStatements
            (Model model, Resource subject, PrintWriter writer )
    {
        if(performedPleasingObjects.contains(subject)) { return; }

        Statement typeStatement = getType(subject);
        StmtIterator sIter = model.listStatements( subject, null, (RDFNode) null );
        boolean isDescription = typeStatement == null;

        if(isDescription) {
            writeDescriptionHeader( subject, writer );
            while (sIter.hasNext()) writePredicate( sIter.nextStatement(), writer );
            writeDescriptionTrailer( subject, writer );
        }
        else {
            writePrettyDescriptionHeader(subject, typeStatement, writer);
            while (sIter.hasNext())
            {
                Statement nextStatement = sIter.nextStatement();
                if(!typeStatement.equals(nextStatement))    //skip type statement
                {
                    writePredicate(nextStatement, writer);
                }
            }
            writePrettyDescriptionTrailer( subject, typeStatement, writer );
        }
    }

    protected void writePrettyDescriptionHeader( Resource subject, Statement stmt, PrintWriter writer)
    {
        Resource r = stmt.getResource();
        writer.print( space + "<" + startElementTag(r.getNameSpace(), r.getLocalName()) + " " );
        writeResourceId( subject, writer );
        writer.println( ">" );
    }

    protected void writePrettyDescriptionTrailer( Resource subject, Statement stmt, PrintWriter writer )
    {
        Resource r = stmt.getResource();
        writer.println( space + "</" + endElementTag(r.getNameSpace(), r.getLocalName()) + ">" );
    }
}
