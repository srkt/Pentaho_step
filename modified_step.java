import java.util.regex.*;

private String currentDate = "No Date"; // Default when no valid date exists
private StringBuilder accumulatedText = new StringBuilder(); // Stores text for the last valid date
private boolean firstRowProcessed = false; // Tracks if a valid date has been processed
private int rowIndex = 0; // Index starts at 0, increments before output (first output is 1)

public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
    try {
        Object[] rowData = getRow();

        // If no more rows, output the last accumulated row and finish
        if (rowData == null) {
            if (accumulatedText.length() > 0) {
                rowIndex++; // Increment index for final row
                Object[] finalRow = createOutputRow(null, data.outputRowMeta.size());
                get(Fields.Out, "index").setValue(finalRow, rowIndex);
                get(Fields.Out, "date").setValue(finalRow, currentDate);
                get(Fields.Out, "text").setValue(finalRow, accumulatedText.toString().trim());
                putRow(data.outputRowMeta, finalRow);
                logBasic("Final Output -> Index: " + rowIndex + ", Date: " + currentDate + ", Text: " + accumulatedText.toString().trim());
            }
            logBasic("Processing completed.");
            setOutputDone();
            return false;
        }

        // Check if field exists and is not null
        String fullText = (rowData.length > 0) ? get(Fields.In, "full_text").getString(rowData) : null;
        if (fullText == null || fullText.trim().isEmpty()) {
            logBasic("Skipping empty or null row.");
            return true;
        }

        // Split text into lines
        String[] lines = fullText.split("\\r?\\n");
        Pattern datePattern = Pattern.compile("^(\\d{1,2}/\\d{1,2}/\\d{2})\\s*-\\s*(.*)$");

        logBasic("Processing input text...");

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue; // Skip empty lines
            }

            logBasic("Reading line: " + line);

            Matcher matcher = datePattern.matcher(line.trim());
            if (matcher.find()) {
                // Valid date found (e.g., 12/10/08)
                if (firstRowProcessed && accumulatedText.length() > 0) {
                    rowIndex++; // Increment index for each output row
                    Object[] outputRow = createOutputRow(rowData, data.outputRowMeta.size());
                    get(Fields.Out, "index").setValue(outputRow, rowIndex);
                    get(Fields.Out, "date").setValue(outputRow, currentDate);
                    get(Fields.Out, "text").setValue(outputRow, accumulatedText.toString().trim());
                    putRow(data.outputRowMeta, outputRow);
                    logBasic("Output Row -> Index: " + rowIndex + ", Date: " + currentDate + ", Text: " + accumulatedText.toString().trim());
                }
                currentDate = matcher.group(1); // e.g., "12/10/08"
                accumulatedText.setLength(0); // Clear previous text
                accumulatedText.append(matcher.group(2).trim()); // e.g., "some text text text"
                logBasic("New Date Found: " + currentDate + " -> Initial Text: " + matcher.group(2).trim());
                firstRowProcessed = true;
            } else {
                // Non-date line or invalid date (e.g., "xx/xx/xx - some text"): append to current text
                if (accumulatedText.length() > 0) {
                    accumulatedText.append(" "); // Space between lines
                }
                accumulatedText.append(line);
                logBasic("Appending text to Date " + currentDate + ": " + line);
            }
        }

    } catch (Exception e) {
        logError("Error processing row: " + e.getMessage(), e);
        throw new KettleException("Error in UDJC step", e);
    }

    return true; // Continue processing
}
