package org.ohdsi.hydra.actionHandlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ohdsi.circe.cohortdefinition.CohortExpression;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder.BuildExpressionQueryOptions;
import org.ohdsi.utilities.InMemoryFile;
import org.ohdsi.utilities.JsonUtilities;

/**
 * Convert a JSON array in the study specifications to a set of SQL files in the study package using Circe.
 */
public class JsonArrayToSql implements ActionHandlerInterface {

	private Map<String, String>	fileNametoSql;
	private Set<String>			done;

	public JsonArrayToSql(JSONObject action, JSONObject studySpecs) {
		fileNametoSql = new HashMap<String, String>();
		JSONArray array = (JSONArray) JsonUtilities.getViaPath(studySpecs, action.getString("input"));
		for (Object elementObject : array) {
			JSONObject element = (JSONObject) elementObject;
			String json = JsonUtilities.getViaPath(element, action.getString("payload")).toString();
			CohortExpression cohortExpression = CohortExpression.fromJson(json);
			CohortExpressionQueryBuilder builder = new CohortExpressionQueryBuilder();
			BuildExpressionQueryOptions options = new BuildExpressionQueryOptions();
			options.generateStats = false;
			String sql = builder.buildExpressionQuery(cohortExpression, options);
			String fileName = JsonUtilities.getViaPath(element, action.getString("fileName")).toString();
			fileName = action.getString("output") + "/" + fileName + ".sql";
			fileNametoSql.put(fileName, sql);
		}
		done = new HashSet<String>(fileNametoSql.size());
	}

	public void modifyExisting(InMemoryFile file) {
		String fileName = file.getName();
		if (fileNametoSql.keySet().contains(fileName))
			if (done.contains(fileName))
				file.setDeleted(true);
			else {
				file.setContent(fileNametoSql.get(fileName));
				done.add(fileName);
			}
	}

	public List<InMemoryFile> generateNew() {
		List<InMemoryFile> files = new ArrayList<InMemoryFile>(1);
		for (String fileName : fileNametoSql.keySet()) {
			if (!done.contains(fileName)) {
				InMemoryFile file = new InMemoryFile(fileName, fileNametoSql.get(fileName));
				files.add(file);
			}
		}
		return files;
	}
}
