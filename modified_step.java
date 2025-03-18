import java.util.regex.*;

private String currentDate = null; // Null when no valid date exists
private StringBuilder accumulatedText = new StringBuilder(); // Stores text for the last valid date
private boolean firstRowProcessed = false; // Tracks if a valid date has been processed
private int rowIndex = 0; // Index starts at 0, increments before output (first output is 1)

public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
    Object[] rowData = getRow();

    // No more rows: output the last accumulated row and finish
    if (rowData == null) {
        if (accumulatedText.length() > 0) {
            rowIndex++; // Increment index for final row
            Object[] finalRow = createOutputRow(null, data != null && data.outputRowMeta != null ? data.outputRowMeta.size() : 0);
            if (finalRow != null) {
                setFieldValue(finalRow, "index", rowIndex);
                setFieldValue(finalRow, "date", currentDate); // Will be null if no valid date
                setFieldValue(finalRow, "text", accumulatedText.toString().trim());
                putRow(data.outputRowMeta, finalRow);
            }
        }
        setOutputDone();
        return false;
    }

    // Get the full text field safely
    String fullText = null;
    try {
        Object fieldValue = get(Fields.In, "full_text") != null ? get(Fields.In, "full_text").getValue(rowData) : null;
        fullText = fieldValue != null ? fieldValue.toString() : null;
    } catch (Exception e) {
        // Log the error if needed, but continue with null
    }
    if (fullText == null || fullText.trim().isEmpty()) {
        return true; // Skip empty or null rows
    }

    // Split text into lines
    String[] lines = fullText.split("\\r?\\n");
    Pattern datePattern = Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2})\\s*-\\s*(.*)$");

    for (String line : lines) {
        if (line == null) {
            continue; // Skip null lines from split
        }
        line = line.trim();
        if (line.isEmpty()) {
            continue; // Skip empty lines
        }

        Matcher matcher = datePattern.matcher(line);
        if (matcher.find()) {
            // Valid date found (e.g., 12/10/08)
            if (accumulatedText.length() > 0) {
                // Output any accumulated text (even if no prior valid date)
                rowIndex++; // Increment index for each output row
                Object[] outputRow = createOutputRow(rowData, data != null && data.outputRowMeta != null ? data.outputRowMeta.size() : 0);
                if (outputRow != null) {
                    setFieldValue(outputRow, "index", rowIndex);
                    setFieldValue(outputRow, "date", currentDate); // Null if no prior valid date
                    setFieldValue(outputRow, "text", accumulatedText.toString().trim());
                    putRow(data.outputRowMeta, outputRow);
                }
            }
            currentDate = matcher.group(1); // e.g., "12/10/08"
            accumulatedText.setLength(0); // Clear previous text
            accumulatedText.append(matcher.group(2).trim()); // e.g., "some text text text"
            firstRowProcessed = true;
        } else {
            // Non-date line or invalid date (e.g., "xx/xx/xx - some text"): append to current text
            if (accumulatedText.length() > 0) {
                accumulatedText.append(" "); // Space between lines
            }
            accumulatedText.append(line);
        }
    }

    return true; // Continue processing
}

private void setFieldValue(Object[] row, String fieldName, Object value) {
    try {
        if (row != null && get(Fields.Out, fieldName) != null) {
            get(Fields.Out, fieldName).setValue(row, value);
        }
    } catch (Exception e) {
        // Log the error if needed, but avoid throwing to prevent stopping the step
    }
}
