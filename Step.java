import java.util.regex.*;
import java.util.ArrayList;

public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
    // Get input values
    String projectId = get(Fields.In, "ProjectId").getString();
    String textColumn = get(Fields.In, "TextColumn").getString();

    if (textColumn == null || textColumn.trim().isEmpty()) {
        return true;  // Skip empty text fields
    }

    // Define the regex pattern to extract date-text pairs (handles inconsistent spaces)
    Pattern pattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{2})\\s*-*\\s*(.*?)(?=(\\d{2}/\\d{2}/\\d{2})|$)");
    Matcher matcher = pattern.matcher(textColumn);

    // Process each match
    while (matcher.find()) {
        String date = matcher.group(1);   // Extract Date
        String text = matcher.group(2).trim();  // Extract Text and trim spaces

        // Create output row
        Object[] outputRow = createOutputRow(getOutputRowMeta().size());
        outputRow[getOutputRowMeta().indexOfValue("ProjectId")] = projectId;
        outputRow[getOutputRowMeta().indexOfValue("Date")] = date;
        outputRow[getOutputRowMeta().indexOfValue("Text")] = text;

        // Send row to next step
        putRow(getOutputRowMeta(), outputRow);
    }

    return true;  // Continue processing rows
}
