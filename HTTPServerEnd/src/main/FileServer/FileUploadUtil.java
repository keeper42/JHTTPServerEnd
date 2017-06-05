package main.FileServer;
// Created by LJF on 2017/6/4.

import javax.xml.ws.spi.http.HttpExchange;
import java.io.*;

public class FileUploadUtil {

    private String fileName;
    private String boundary;    // Used to determine the boundaries of the index file
    private String contentType;

    public void flieUpload(HttpExchange httpExchange, String path){

        // Here I will achieve the file upload function...

    }

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

    /**
     * The following functions refer to some blogs.
     * @param buffer
     * @return
     */

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

    public static int getIndexOf(byte[] source ,byte[] part){
        if (source == null || part == null || source.length == 0 || part.length == 0){
            return -1;
        }
        int i,j;
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
