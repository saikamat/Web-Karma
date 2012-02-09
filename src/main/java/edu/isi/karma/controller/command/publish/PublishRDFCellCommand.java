package edu.isi.karma.controller.command.publish;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.isi.karma.controller.command.Command;
import edu.isi.karma.controller.command.CommandException;
import edu.isi.karma.controller.update.AbstractUpdate;
import edu.isi.karma.controller.update.ErrorUpdate;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.modeling.alignment.Alignment;
import edu.isi.karma.modeling.alignment.AlignmentManager;
import edu.isi.karma.modeling.alignment.LabeledWeightedEdge;
import edu.isi.karma.modeling.alignment.Vertex;
import edu.isi.karma.rdf.SourceDescription;
import edu.isi.karma.rdf.WorksheetRDFGenerator;
import edu.isi.karma.view.VWorkspace;

public class PublishRDFCellCommand extends Command {
	private final String vWorksheetId;
	private final String nodeId;
	private String rdfSourcePrefix;
	//rdf for this cell
	private StringWriter outRdf = new StringWriter();


	public enum JsonKeys {
		updateType, cellRdf, vWorksheetId
	}

	private static Logger logger = LoggerFactory
			.getLogger(PublishRDFCellCommand.class);

	protected PublishRDFCellCommand(String id, String vWorksheetId,
			String nodeId, String rdfSourcePrefix) {
		super(id);
		this.vWorksheetId = vWorksheetId;
		this.nodeId = nodeId;
		this.rdfSourcePrefix = rdfSourcePrefix;
	}

	@Override
	public String getCommandName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String getTitle() {
		return "Publish RDF Cell";
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public CommandType getCommandType() {
		return CommandType.notInHistory;
	}

	@Override
	public UpdateContainer doIt(VWorkspace vWorkspace) throws CommandException {

		// get alignment for this worksheet
		logger.info("Get alignment for " + vWorksheetId);
		Alignment alignment = AlignmentManager.Instance().getAlignment(
				vWorkspace.getWorkspace().getId() + ":" + vWorksheetId + "AL");
		if (alignment == null) {
			logger.info("Alignment is NULL for " + vWorksheetId);
			return new UpdateContainer(new ErrorUpdate(
					"Worksheet not modeled!"));
		}

		DirectedWeightedMultigraph<Vertex, LabeledWeightedEdge> tree = alignment
				.getSteinerTree();
		Vertex root = alignment.GetTreeRoot();

        try {
			if (root != null) {
				// Write the source description
				// use true to generate a SD with column names (for use
				// "outside" of Karma)
				// use false for internal use
				SourceDescription desc = new SourceDescription(
						vWorkspace.getRepFactory(), tree, root,
						rdfSourcePrefix, true,false);
				String descString = desc.generateSourceDescription();
				logger.info("SD=" + descString);
			    PrintWriter outWriter = new PrintWriter (outRdf);
				WorksheetRDFGenerator wrg = new WorksheetRDFGenerator(
						vWorkspace.getRepFactory(), descString, outWriter);
				wrg.generateTriplesCell(nodeId);
				// //////////////////

			} else {
				return new UpdateContainer(new ErrorUpdate(
						"Alignment returned null root!!"));
			}

			return new UpdateContainer(new AbstractUpdate() {
				@Override
				public void generateJson(String prefix, PrintWriter pw,
						VWorkspace vWorkspace) {
					JSONObject outputObject = new JSONObject();
					try {
						outputObject.put(JsonKeys.updateType.name(),
								"PublishCellRDFUpdate");
						outputObject.put(JsonKeys.cellRdf.name(),
								outRdf.toString().replaceAll("\\n", "<br />"));
						outputObject.put(JsonKeys.vWorksheetId.name(),
								vWorksheetId);
						pw.println(outputObject.toString(4));
					} catch (JSONException e) {
						logger.error("Error occured while generating JSON!");
					}
				}
			});
		} catch (Exception e) {
			return new UpdateContainer(new ErrorUpdate(e.getMessage()));
		}
	}

	@Override
	public UpdateContainer undoIt(VWorkspace vWorkspace) {
		// TODO Auto-generated method stub
		return null;
	}

}
