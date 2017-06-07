package main.FileServer;
// Created by LJF on 2017/6/4.

import java.io.*;
import com.sun.net.httpserver.HttpExchange;

public class FileUploadUtil {

    private String fileName;
    private String boundary;    // Used to determine the boundaries of the index file
    private String contentType;

    public FileUploadUtil(String fileName, String boundary, String contentType){
        this.fileName = fileName;
        this.boundary = boundary;
        this.contentType = contentType;
    }

    public FileUploadUtil(){}

    public void flieUpload(HttpExchange httpExchange, String path){

        try{
            InputStream is = httpExchange.getRequestBody();
            BufferedInputStream bis = new BufferedInputStream(is);
            int len = 0;
            boolean isStart = true;
            byte[] buffer = new byte[1024];
            OutputStream os = null;
            while((len = bis.read(buffer)) != -1){
                // Judgment is not the delimiter of the file, if not, read the temporary file.
                if(isStart) {
                    int startReadIndex = getFileReadIndex(buffer);
                    isStart = false;
                    if (fileName != null) {
                        File file = new File(path, fileName);
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        os = new FileOutputStream(file);
                        byte[] realData = cartByte(buffer, startReadIndex, buffer.length - startReadIndex);
                        String dataStr = new String(realData);
                        if (dataStr.contains("-")) {
                            realData = cartByte(realData, 0, getIndexOf(realData, "-".getBytes()) - "\r\n-".getBytes().length);
                            os.write(realData);
                            os.close();
                            break;
                        }
                        os.write(buffer, startReadIndex, buffer.length - startReadIndex);
                        continue;
                    }
                }
                if(os == null){
                    break;
                }
                // If buffer includes the terminator, the outputstream reads the previous data only.
                String bufferStr = new String(buffer);
                if(!bufferStr.contains(boundary)){
                    os.write(buffer, 0, len);
                }else{
                    buffer = cartByte(buffer, 0, getIndexOf(buffer, boundary.getBytes()) - 2);
                    os.write(buffer, 0, buffer.length - 1);
                    os.close();
                    break;
                }
                System.out.println("Filename: " + fileName);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * The following functions refer to some blogs.
     */

    public int getFileReadIndex(byte[] buffer){
        // Read four '\n' symbols to check if it is a file
        for(int i = 0; i < buffer.length; i++){
            int startIndex = getLineIndex(buffer, 4 * i);
            int endIndex = getLineIndex(buffer, 4 * (i+1));
            byte[] fourLines = cartByte(buffer, startIndex, endIndex);
            if(isFileBoundary(fourLines)){
                return endIndex;
            }
        }
        return 0;
    }

    public boolean isFileBoundary(byte[] buffer){
        String fileItem = new String(buffer);
        if(fileItem.contains("filename")){
            boundary  = fileItem.substring(0,fileItem.indexOf("\n") - 1);
            fileItem = fileItem.substring(fileItem.indexOf("\n") + 1,fileItem.length());
            fileName = fileItem.substring(fileItem.indexOf("filename=\"") + "filename=\"".length(), fileItem.indexOf("\n") - "\"\n".length());
            contentType = fileItem.substring(fileItem.indexOf("Content-Type:"),fileItem.length());
            return true;
        }
        return false;
    }

    private int getLineIndex(byte[] source, int lineNumber){
        if(lineNumber <= 0){
            return 0;
        }
        int lineCount = 0;
        for( int k = 0;k < source.length;k++){
            if( lineCount == lineNumber )
                return k;
            if( source[k] == "\n".getBytes()[0] && lineCount  <= lineNumber)
                lineCount ++;
        }
        return 0;
    }

    /**
     * Get byte [] to specify the starting position of the array.
     * @param source
     * @param beginIndex
     * @param endIndex
     * @return
     */
    public byte[] cartByte(byte[] source, int beginIndex, int endIndex){

        if(source == null || source.length <= 0 || endIndex - beginIndex <= 0){
            return null;
        }
        int byteLength = (endIndex + 1) - beginIndex;
        byte[] returnData = new byte[byteLength];
        for(int i = 0; i < byteLength; i++){
            returnData[i] = source[i + beginIndex];
        }
        return returnData;
    }

    /**
     * Gets the position of a string of byte arrays in the original byte array.
     * @param source
     * @param part
     * @return
     */
    public static int getIndexOf(byte[] source ,byte[] part){
        if (source == null || part == null || source.length == 0 || part.length == 0){
            return -1;
        }
        int i, j;
        for(i = 0;i < source.length; i++){
            if(source[i] == part[0]){
                for(j = 0;j < part.length; j++)
                    if(source[i+j] != part[j]) break;
                if(j == part.length) return i;
            }
        }
        return -1;
    }
}
