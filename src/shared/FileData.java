package shared;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * FileData class for transferring files over network
 * Contains file metadata and content in serializable format
 */
public class FileData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public String fileName;     // Original file name
    public byte[] fileContent;  // File content as byte array
    public long fileSize;       // File size in bytes

    /**
     * Constructor - Reads file and converts to transferable format
     * @param file The file to be transferred
     * @throws IOException if file cannot be read
     */
    public FileData(File file) throws IOException {
        this.fileName = file.getName();
        this.fileSize = file.length();
        
        // Read entire file into byte array
        try (FileInputStream fis = new FileInputStream(file)) {
            this.fileContent = fis.readAllBytes();
        }
    }

    /**
     * Get file size in KB for display purposes
     * @return file size in kilobytes
     */
    public double getFileSizeKB() {
        return fileSize / 1024.0;
    }

    /**
     * Get file size in MB for display purposes
     * @return file size in megabytes
     */
    public double getFileSizeMB() {
        return fileSize / (1024.0 * 1024.0);
    }

    @Override
    public String toString() {
        return "FileData{" +
                "fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize + " bytes" +
                '}';
    }
}