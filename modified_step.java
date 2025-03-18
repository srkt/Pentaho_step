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

        // Define regex to match valid MM/dd/yy dates (only numbers)
        Pattern validDatePattern = Pattern.compile("(0[1-9]|1[0-2])/(0[1-9]|[12][0-9]|3[01])/\\d{2}");
        // Match both valid and invalid dates
        Pattern datePattern = Pattern.compile("(\\d{2}|xx)/(\\d{2}|xx)/(\\d{2})");
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

            // Add the matched date itself (valid or invalid)
            dateSegments.add(matcher.group(0));

            // Move the last index forward
            lastIndex = matcher.end();
        }

        // Add remaining text if any
        if (lastIndex < statusText.length()) {
            dateSegments.add(statusText.substring(lastIndex).trim());
        }

        logBasic("Extracted Segments: " + dateSegments.toString());

        // Iterate over extracted segments and determine date-text pairs
        for (int i = 0; i < dateSegments.size(); i++) {
            String segment = dateSegments.get(i);
            Matcher validDateMatcher = validDatePattern.matcher(segment);
            Matcher anyDateMatcher = datePattern.matcher(segment);

            if (validDateMatcher.matches()) {
                // This is a valid date
                if (i + 1 < dateSegments.size()) {
                    // The next segment is the corresponding text
                    String cleanedText = dateSegments.get(i + 1).replaceAll("^[\\s\\-:]+", "").trim();
                    
                    if (!cleanedText.isEmpty()) {
                        Object[] outputRow = createOutputRow(rowData, data.outputRowMeta.size());
                        get(Fields.Out, "date").setValue(outputRow, segment);
                        get(Fields.Out, "text").setValue(outputRow, cleanedText);
                        putRow(data.outputRowMeta, outputRow);

                        logBasic("Output Row -> Date: " + segment + ", Text: " + cleanedText);
                    }
                    i++; // Skip next segment since it's already processed
                }
            } else if (anyDateMatcher.matches()) {
                // This is an invalid date (contains "xx")
                String invalidDateText = segment;
                
                if (i + 1 < dateSegments.size()) {
                    // Append the corresponding text
                    invalidDateText += " " + dateSegments.get(i + 1);
                    i++; // Skip next segment
                }

                invalidDateText = invalidDateText.replaceAll("^[\\s\\-:]+", "").trim();

                if (!invalidDateText.isEmpty()) {
                    Object[] outputRow = createOutputRow(rowData, data.outputRowMeta.size());
                    get(Fields.Out, "date").setValue(outputRow, null); // Invalid date -> null
                    get(Fields.Out, "text").setValue(outputRow, invalidDateText);
                    putRow(data.outputRowMeta, outputRow);

                    logBasic("Output Row -> Date: NULL, Text: " + invalidDateText);
                }
            }
        }
    } catch (Exception e) {
        logError("Error processing row: " + e.getMessage(), e);
        throw new KettleException("Error in UDJC step", e);
    }

    return true; // Continue processing
}
