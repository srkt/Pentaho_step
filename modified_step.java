import java.util.regex.*;

private String currentDate = "No Date"; // Default when no valid date exists
private StringBuilder accumulatedText = new StringBuilder(); // Stores text for the last valid date
private boolean firstRowProcessed = false; // Tracks if a valid date has been processed
private int rowIndex = 0; // Index starts at 0, increments before output (first output is 1)

public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
    Object[] rowData = getRow();

    // No more rows: output the last accumulated row and finish
    if (rowData == null) {
        if (accumulatedText.length() > 0) {
            rowIndex++; // Increment index for final row
            Object[] finalRow = createOutputRow(null, data.outputRowMeta.size());
            get(Fields.Out, "index").setValue(finalRow, rowIndex);
            get(Fields.Out, "date").setValue(finalRow, currentDate);
            get(Fields.Out, "text").setValue(finalRow, accumulatedText.toString().trim());
            putRow(data.outputRowMeta, finalRow);
        }
        setOutputDone();
        return false;
    }

    // Get the full text field
    String fullText = get(Fields.In, "full_text").getString(rowData);
    if (fullText == null || fullText.trim().isEmpty()) {
        return true; // Skip empty rows
    }

    // Split text into lines
    String[] lines = fullText.split("\\r?\\n");
    Pattern datePattern = Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2})\\s*-\\s*(.*)$");

    for (String line : lines) {
        line = line.trim();
        if (line.isEmpty()) {
            continue; // Skip empty lines
        }

        Matcher matcher = datePattern.matcher(line);
        if (matcher.find()) {
            // Valid date found (e.g., 12/10/08)
            if (firstRowProcessed && accumulatedText.length() > 0) {
                rowIndex++; // Increment index for each output row
                Object[] outputRow = createOutputRow(rowData, data.outputRowMeta.size());
                get(Fields.Out, "index").setValue(outputRow, rowIndex);
                get(Fields.Out, "date").setValue(outputRow, currentDate);
                get(Fields.Out, "text").setValue(outputRow, accumulatedText.toString().trim());
                putRow(data.outputRowMeta, outputRow);
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
