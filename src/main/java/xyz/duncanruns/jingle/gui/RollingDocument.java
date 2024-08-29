package xyz.duncanruns.jingle.gui;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class RollingDocument extends PlainDocument {

    private int maxLogChars = 75000;
    private String truncateMessage = "Logs have been truncated.\n\n";
    private boolean truncateMessageAdded = false;

    public void setMaxLogChars(int maxLogChars) {
        this.maxLogChars = maxLogChars;
    }

    public void setTruncateMessage(String truncateMessage) {
        this.truncateMessage = truncateMessage;
    }

    public synchronized void addLineWithRolling(String line) {
        line = line.trim();
        if (this.getLength() > 0) {
            line = "\n" + line;
        }

        try {
            this.insertString(this.getLength(), line, null);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }

        int totalTextLength = this.getLength();
        if (totalTextLength > this.maxLogChars) {
            try {
                if (!this.truncateMessageAdded) {
                    this.insertString(0, this.truncateMessage, null);
                    this.truncateMessageAdded = true;
                }
                int truncateMessageLength = this.truncateMessage.length();
                int toRemove = this.getText(truncateMessageLength, truncateMessageLength + totalTextLength - this.maxLogChars).lastIndexOf("\n");
                if (toRemove == -1) return;
                toRemove += 1; // Include the newline itself for removal, and then remove logOffset

                this.remove(truncateMessageLength, toRemove);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
