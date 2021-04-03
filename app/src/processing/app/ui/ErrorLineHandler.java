package processing.app;
import processing.app.Problem;
import java.util.*;
import java.io.*;

public class ErrorLineHandler{
    List<Problem> problems;
    String problemLine;
    String problemVariableType;
    String problemFunction;

    public ErrorLineHandler(List<Problem> p){
        problems = p;
    }

    public int getProblemLineNumber(){
        return problems.get(0).getLineNumber();
    }

    public void handleLine(String line){
        problemLine = line;
        String[] lineArray = line.split(" ");
        problemVariableType = lineArray[1];
        problemFunction = getFunctionName(lineArray[2]);
    }

    public String getFunctionName(String s){
        char[] arr = s.toCharArray();
        for(int i = 0; i < arr.length; i++){
            if(arr[i] == '(') return s.substring(0, i);
        }
        return "";
    }

    public String getProblemLine(){
        return problemLine;
    }

    public String getProblemVariableType(){
        return problemVariableType;
    }

    public String getProblemFunction(){
        return problemFunction;
    }
}
