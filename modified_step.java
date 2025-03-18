import java.util.regex.*;
import java.util.ArrayList;
import java.util.List;

public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
    try {
        Object[] rowData = getRow();
        
        // If no more rows, stop processing
        if (rowData == null) {
            setOutputDone();
            return false;
        }

        // Get the status field from the database row
        String statusText = get(Fields.In, "status").getString(rowData);
        if (statusText == null || statusText.trim().isEmpty()) {
            return true; // Skip empty rows
        }

        logBasic("Processing Status: " + statusText);

        // Define regex to match ONLY valid MM/dd/yy dates
        Pattern datePattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{2})");
        Matcher matcher = datePattern.matcher(statusText);

        // List to store extracted date-text pairs
        List<String> dateSegments = new ArrayList<>();

        int lastIndex = 0;

        // Iterate over all matches
        while (matcher.find()) {
            int matchStart = matcher.start();

            // Extract text before the current date
            if (matchStart > lastIndex) {
                dateSegments.add(statusText.substring(lastIndex, matchStart).trim());
            }

            // Add the matched date itself
            dateSegments.add(matcher.group(1));

            // Move the last index forward
            lastIndex = matcher.end();
        }

        // Add remaining text if any
        if (lastIndex < statusText.length()) {
            dateSegments.add(statusText.substring(lastIndex).trim());
        }

        logBasic("Extracted Segments: " + dateSegments.toString());

        // Iterate over extracted segments and determine date-text pairs
        String currentDate = null;

        for (String segment : dateSegments) {
            Matcher dateMatcher = datePattern.matcher(segment);

            if (dateMatcher.matches()) {
                // This segment is a valid date
                currentDate = segment;
            } else {
                // This segment is text
                Object[] outputRow = createOutputRow(rowData, data.outputRowMeta.size());
                get(Fields.Out, "date").setValue(outputRow, (currentDate != null) ? currentDate : null);
                get(Fields.Out, "text").setValue(outputRow, segment);
                putRow(data.outputRowMeta, outputRow);

                logBasic("Output Row -> Date: " + (currentDate != null ? currentDate : "NULL") + ", Text: " + segment);
            }
        }
    } catch (Exception e) {
        logError("Error processing row: " + e.getMessage(), e);
        throw new KettleException("Error in UDJC step", e);
    }

    return true; // Continue processing
}
