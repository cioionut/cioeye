package com.cioeye;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

public class TextFileFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
        ArrayList<String> extensions = new ArrayList<String>(
                Arrays.asList(".txt", ".html", ".pdf"));
        for (String extension :extensions) {
            if (pathname.getName().toLowerCase().endsWith(extension))
                    return true;
        }
        return false;
    }
}