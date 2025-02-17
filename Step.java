import java.util.regex.*;
import java.util.ArrayList;

public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
    // Get input values
    String projectId = get(Fields.In, "ProjectId").getString();
    String textColumn = get(Fields.In, "TextColumn").getString();
    
    // Define the regex pattern to extract date-text pairs
    Pattern pattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{2})\\s*-*\\s*(.*?)(?=(\\d{2}/\\d{2}/\\d{2})|$)");
    Matcher matcher = pattern.matcher(textColumn);

    // Store extracted values
    ArrayList<Object[]> rows = new ArrayList<>();

    while (matcher.find()) {
        String date = matcher.group(1);  // Extract Date
        String text = matcher.group(2).trim();  // Extract Text and trim spaces

        // Add extracted row to list
        rows.add(new Object[]{projectId, date, text});
    }

    // Output each extracted row
    for (Object[] row : rows) {
        Object[] outputRow = createOutputRow(rowMeta.size());
        outputRow[0] = row[0]; // ProjectId
        outputRow[1] = row[1]; // Date
        outputRow[2] = row[2]; // Text
        putRow(outputRow);
    }

    // Stop processing after processing all rows
    return false;
}
